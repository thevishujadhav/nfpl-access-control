package com.nfpl.access.config;

import com.nfpl.access.domain.*;
import com.nfpl.access.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "app.dataset", havingValue = "sample", matchIfMissing = true)
public class SeedData {
    @Bean CommandLineRunner seed(StaffUserRepository users, JobRepository jobs,
                                  PermissionGrantRepository grants, PasswordEncoder encoder) {
        return args -> {
            if (users.count()>0) return;
            addUser(users,encoder,"admin","Admin123!","System Admin","ADMIN");
            addUser(users,encoder,"rahul","Rahul123!","Rahul Maker","STAFF");
            addUser(users,encoder,"priya","Priya123!","Priya Checker","STAFF");
            addUser(users,encoder,"clientabc","Client123!","ABC Client User","CLIENT");
            grant(grants,"admin",PermissionRole.ADMIN,"*","*","*","*","admin");
            grant(grants,"rahul",PermissionRole.MAKER,"*","*","*","*","admin");
            grant(grants,"priya",PermissionRole.CHECKER,"ABC","MUMBAI","IMPORT","*","admin");
            grant(grants,"clientabc",PermissionRole.CLIENT_VIEWER,"ABC","MUMBAI","IMPORT","*","admin");
            createJob(jobs,"Import laptops","ABC","MUMBAI",Direction.IMPORT,ShipmentMode.AIR,"rahul",JobStage.CONFIRMED);
            createJob(jobs,"Export textiles","XYZ","PUNE",Direction.EXPORT,ShipmentMode.FCL,"rahul",JobStage.CONFIRMED);
        };
    }
    private void addUser(StaffUserRepository repo,PasswordEncoder enc,String username,String password,String name,String type) {
        StaffUser u=new StaffUser(); u.setUsername(username); u.setPasswordHash(enc.encode(password));
        u.setDisplayName(name); u.setAccountType(type); repo.save(u);
    }
    private void grant(PermissionGrantRepository repo,String user,PermissionRole role,String client,String branch,
                       String direction,String mode,String by) {
        PermissionGrant g=new PermissionGrant(); g.setUsername(user); g.setRole(role);
        g.setClientCode(client); g.setBranchCode(branch); g.setDirection(direction); g.setMode(mode);
        g.setGrantedBy(by); repo.save(g);
    }
    private void createJob(JobRepository repo,String title,String client,String branch,Direction direction,
                           ShipmentMode mode,String owner,JobStage stage) {
        JobRecord j=new JobRecord(); j.setTitle(title); j.setClientCode(client); j.setBranchCode(branch);
        j.setDirection(direction); j.setMode(mode); j.setOwnerUsername(owner); j.setStage(stage); repo.save(j);
    }
}
