package com.nfpl.access;

import com.nfpl.access.domain.*;
import com.nfpl.access.repo.*;
import com.nfpl.access.service.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccessControlApplicationTests {
    @Autowired JobRepository jobs;
    @Autowired AccessService access;

    @Test void makerCanPrepareJobInScope() {
        JobRecord job=jobs.findAll().stream().filter(j->j.getClientCode().equals("ABC")).findFirst().orElseThrow();
        assertThat(access.check("rahul",job,"PREPARE").allowed()).isTrue();
    }
    @Test void makerCannotCheckOwnJob() {
        JobRecord job=jobs.findAll().stream().filter(j->j.getClientCode().equals("ABC")).findFirst().orElseThrow();
        job.setStage(JobStage.PREPARED); jobs.save(job);
        assertThat(access.check("rahul",job,"CHECK").reasonCode()).isEqualTo("MAKER_CHECKER_CONFLICT");
    }
    @Test void checkerScopeDoesNotCoverOtherClient() {
        JobRecord job=jobs.findAll().stream().filter(j->j.getClientCode().equals("XYZ")).findFirst().orElseThrow();
        job.setStage(JobStage.PREPARED); jobs.save(job);
        assertThat(access.check("priya",job,"CHECK").allowed()).isFalse();
    }
}
