package com.nfpl.access.config;

import com.nfpl.access.domain.*;
import com.nfpl.access.repo.PermissionGrantRepository;
import com.nfpl.access.repo.StaffUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Generates a repeatable 500-user / 200,000-job dataset in the configured database. */
@Configuration
@ConditionalOnProperty(name = "app.dataset", havingValue = "scale")
public class ScaleDataGenerator {
    private static final int USER_COUNT = 500;
    private static final int JOB_COUNT = 200_000;
    private static final int BATCH_SIZE = 1_000;
    private static final String[] CLIENTS = {"C001","C002","C003","C004","C005","C006","C007","C008","C009","C010"};
    private static final String[] BRANCHES = {"MUMBAI","PUNE","DELHI","CHENNAI","BENGALURU"};
    private static final String[] MODES = {"AIR","FCL","LCL"};
    private static final String[] DIRECTIONS = {"IMPORT","EXPORT"};
    private static final String[] STAGES = {"CONFIRMED","PREPARED","CHECKED","FILED","CANCELLED"};

    @Bean
    CommandLineRunner generateScaleDataset(StaffUserRepository users,
                                           PermissionGrantRepository grants,
                                           PasswordEncoder encoder,
                                           JdbcTemplate jdbc) {
        return args -> {
            if (users.count() > 0) {
                System.out.println("Scale dataset not generated: database already contains users. Use a fresh database URL.");
                return;
            }

            System.out.println("Generating NFPL scale dataset: 500 users, scoped grants, 200,000 jobs...");
            String encodedPassword = encoder.encode("ScaleDemo123!");
            List<String> staffUsernames = new ArrayList<>();

            for (int i = 1; i <= USER_COUNT; i++) {
                String username;
                String name;
                String accountType;
                if (i == 1) { username = "admin"; name = "Scale Administrator"; accountType = "ADMIN"; }
                else if (i == 2) { username = "parser"; name = "Overnight Parser Service"; accountType = "SERVICE"; }
                else if (i == 3) { username = "rahul"; name = "Rahul Maker"; accountType = "STAFF"; }
                else if (i == 4) { username = "priya"; name = "Priya Checker"; accountType = "STAFF"; }
                else if (i == 5) { username = "clientabc"; name = "Client C001 User"; accountType = "CLIENT"; }
                else {
                    username = String.format("user%03d", i);
                    name = "NFPL User " + String.format("%03d", i);
                    accountType = (i % 50 == 0) ? "CLIENT" : "STAFF";
                }
                StaffUser u = new StaffUser();
                u.setUsername(username);
                u.setPasswordHash(encodedPassword);
                u.setDisplayName(name);
                u.setAccountType(accountType);
                users.save(u);
                if ("STAFF".equals(accountType)) staffUsernames.add(username);

                if (i == 1) {
                    addGrant(grants, username, PermissionRole.ADMIN, "*", "*", "*", "*", "admin");
                } else if (i == 2) {
                    addGrant(grants, username, PermissionRole.PARSER, "*", "*", "*", "*", "admin");
                } else if (i == 3) {
                    addGrant(grants, username, PermissionRole.MAKER, "*", "*", "*", "*", "admin");
                } else if (i == 4) {
                    addGrant(grants, username, PermissionRole.CHECKER, "C001", "MUMBAI", "IMPORT", "*", "admin");
                    addGrant(grants, username, PermissionRole.CHECKER, "C004", "PUNE", "IMPORT", "*", "admin");
                } else if ("CLIENT".equals(accountType)) {
                    String client = (i == 5) ? "C001" : CLIENTS[(i / 50) % CLIENTS.length];
                    addGrant(grants, username, PermissionRole.CLIENT_VIEWER, client, "*", "*", "*", "admin");
                } else {
                    int scope = i % CLIENTS.length;
                    if (i % 3 == 0) addGrant(grants, username, PermissionRole.MAKER, "*", "*", "*", "*", "admin");
                    else addGrant(grants, username, PermissionRole.MAKER, CLIENTS[scope], BRANCHES[i % BRANCHES.length], "*", "*", "admin");
                    if (i % 7 == 0) addGrant(grants, username, PermissionRole.CHECKER, CLIENTS[(scope + 2) % CLIENTS.length], BRANCHES[(i + 1) % BRANCHES.length], "IMPORT", MODES[i % MODES.length], "admin");
                }
            }

            // Add indexes already declared by JPA entities; insert jobs in bounded batches to avoid
            // holding 200,000 entities in memory. H2 and PostgreSQL both support this JDBC approach.
            String sql = "INSERT INTO jobs (title, client_code, branch_code, direction, mode, stage, owner_username, archived, version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            Random random = new Random(20261029L);
            Instant now = Instant.now();
            int inserted = 0;
            while (inserted < JOB_COUNT) {
                int size = Math.min(BATCH_SIZE, JOB_COUNT - inserted);
                List<Object[]> batch = new ArrayList<>(size);
                for (int j = 0; j < size; j++) {
                    int jobNo = inserted + j + 1;
                    String client = CLIENTS[random.nextInt(CLIENTS.length)];
                    String branch = BRANCHES[random.nextInt(BRANCHES.length)];
                    String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                    String mode = MODES[random.nextInt(MODES.length)];
                    String stage = STAGES[random.nextInt(STAGES.length)];
                    String owner = staffUsernames.get(random.nextInt(staffUsernames.size()));
                    batch.add(new Object[]{"Shipment Job " + jobNo, client, branch, direction, mode, stage,
                            owner, false, 0L, Timestamp.from(now.minusSeconds(random.nextLong(365L * 24 * 3600))), Timestamp.from(now)});
                }
                jdbc.batchUpdate(sql, batch, size, (PreparedStatement ps, Object[] row) -> {
                    ps.setString(1, (String) row[0]); ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]); ps.setString(4, (String) row[3]);
                    ps.setString(5, (String) row[4]); ps.setString(6, (String) row[5]);
                    ps.setString(7, (String) row[6]); ps.setBoolean(8, (Boolean) row[7]);
                    ps.setLong(9, (Long) row[8]); ps.setTimestamp(10, (Timestamp) row[9]);
                    ps.setTimestamp(11, (Timestamp) row[10]);
                });
                inserted += size;
                if (inserted % 10_000 == 0) System.out.println("Generated jobs: " + inserted + "/" + JOB_COUNT);
            }
            System.out.println("Scale dataset complete: " + users.count() + " users, " + grants.count() + " grants, " + JOB_COUNT + " jobs.");
            System.out.println("Development login: admin / ScaleDemo123! (all generated users share this demo password)");
        };
    }

    private void addGrant(PermissionGrantRepository repo, String username, PermissionRole role,
                          String client, String branch, String direction, String mode, String grantedBy) {
        PermissionGrant grant = new PermissionGrant();
        grant.setUsername(username); grant.setRole(role); grant.setClientCode(client);
        grant.setBranchCode(branch); grant.setDirection(direction); grant.setMode(mode);
        grant.setGrantedBy(grantedBy); repo.save(grant);
    }
}
