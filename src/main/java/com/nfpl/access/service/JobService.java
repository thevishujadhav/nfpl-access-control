package com.nfpl.access.service;

import com.nfpl.access.domain.*;
import com.nfpl.access.repo.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {
    private final JobRepository jobs; private final AccessService access; private final AuditEventRepository audits;
    public JobService(JobRepository jobs, AccessService access, AuditEventRepository audits) {
        this.jobs=jobs; this.access=access; this.audits=audits;
    }
    public Page<JobRecord> list(String username, Pageable pageable) {
        // Filter every candidate using the same access decision used by action endpoints.
        Page<JobRecord> candidates=jobs.findByArchivedFalse(pageable);
        return new PageImpl<>(candidates.getContent().stream()
            .filter(j -> access.check(username,j,"VIEW").allowed()).toList(), pageable, candidates.getTotalElements());
    }
    public JobRecord getVisible(String username, Long id) {
        JobRecord j=jobs.findById(id).orElseThrow(()->new IllegalArgumentException("Job not found"));
        if (!access.check(username,j,"VIEW").allowed()) throw new org.springframework.security.access.AccessDeniedException("Access denied");
        return j;
    }
    @Transactional
    public JobRecord transition(String username, Long id, String action, String reason) {
        JobRecord j=jobs.findById(id).orElseThrow(()->new IllegalArgumentException("Job not found"));
        AccessDecision decision=access.check(username,j,action);
        if (!decision.allowed()) throw new org.springframework.security.access.AccessDeniedException(decision.reasonCode()+": "+decision.message());
        switch(action) {
            case "PREPARE" -> { j.setStage(JobStage.PREPARED); j.setOwnerUsername(username); }
            case "CHECK" -> j.setStage(JobStage.CHECKED);
            case "FILE" -> j.setStage(JobStage.FILED);
            case "CANCEL" -> j.setStage(JobStage.CANCELLED);
            case "REOPEN" -> j.setStage(JobStage.CONFIRMED);
            default -> throw new IllegalArgumentException("Unsupported transition");
        }
        jobs.save(j);
        AuditEvent event=new AuditEvent(); event.setActorUsername(username); event.setJobId(j.getId());
        event.setAction(action); event.setAllowed(true);
        event.setReason("Job transition completed. Reason: "+(reason==null||reason.isBlank()?"not supplied":reason));
        audits.save(event);
        return j;
    }
}
