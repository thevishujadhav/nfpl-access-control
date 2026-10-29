package com.nfpl.access.api;

import com.nfpl.access.domain.*;
import com.nfpl.access.repo.*;
import com.nfpl.access.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final JobRepository jobs; private final StaffUserRepository users;
    private final PermissionGrantRepository grants; private final AuditEventRepository audits;
    private final AccessService access; private final JobService jobService;

    public ApiController(JobRepository jobs, StaffUserRepository users, PermissionGrantRepository grants,
                         AuditEventRepository audits, AccessService access, JobService jobService) {
        this.jobs=jobs; this.users=users; this.grants=grants; this.audits=audits;
        this.access=access; this.jobService=jobService;
    }
    public record CreateJob(@NotBlank String title,@NotBlank String clientCode,@NotBlank String branchCode,
                            @NotNull Direction direction,@NotNull ShipmentMode mode,@NotBlank String ownerUsername) {}
    public record GrantRequest(@NotBlank String username,@NotNull PermissionRole role,
                               String clientCode,String branchCode,String direction,String mode) {}
    public record CheckRequest(@NotNull Long jobId,@NotBlank String action) {}
    public record TransitionRequest(@NotBlank String action,String reason) {}

    @GetMapping("/me") public Map<String,Object> me(Authentication auth) {
        StaffUser u=users.findByUsername(auth.getName()).orElseThrow();
        return Map.of("username",u.getUsername(),"displayName",u.getDisplayName(),"active",u.isActive(),"absent",u.isAbsent());
    }
    @GetMapping("/jobs") public Page<JobRecord> list(Authentication auth,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return jobService.list(auth.getName(),PageRequest.of(page,Math.min(Math.max(size,1),100),Sort.by("id").ascending()));
    }
    @GetMapping("/jobs/{id}") public JobRecord get(Authentication auth,@PathVariable Long id) {
        return jobService.getVisible(auth.getName(),id);
    }
    @PostMapping("/jobs") @PreAuthorize("hasRole('ADMIN')")
    public JobRecord createJob(@Valid @RequestBody CreateJob req) {
        JobRecord j=new JobRecord(); j.setTitle(req.title()); j.setClientCode(req.clientCode());
        j.setBranchCode(req.branchCode()); j.setDirection(req.direction()); j.setMode(req.mode());
        j.setOwnerUsername(req.ownerUsername()); return jobs.save(j);
    }
    @PostMapping("/access/check") public AccessDecision check(Authentication auth,@Valid @RequestBody CheckRequest req) {
        JobRecord job=jobs.findById(req.jobId()).orElseThrow(()->new IllegalArgumentException("Job not found"));
        return access.check(auth.getName(),job,req.action().trim().toUpperCase());
    }
    @PostMapping("/jobs/{id}/transition") public JobRecord transition(Authentication auth,@PathVariable Long id,
        @Valid @RequestBody TransitionRequest req) {
        return jobService.transition(auth.getName(),id,req.action().trim().toUpperCase(),req.reason());
    }
    @GetMapping("/audit") @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditEvent> audit(@RequestParam(required=false) Long jobId,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        Pageable p=PageRequest.of(page,Math.min(Math.max(size,1),100),Sort.by("occurredAt").descending());
        return jobId==null?audits.findAllByOrderByOccurredAtDesc(p):audits.findByJobIdOrderByOccurredAtDesc(jobId,p);
    }
    @GetMapping("/admin/grants") @PreAuthorize("hasRole('ADMIN')")
    public List<PermissionGrant> grants() { return grants.findAll(); }

    @PostMapping("/admin/grants") @PreAuthorize("hasRole('ADMIN')")
    public PermissionGrant createGrant(Authentication auth,@Valid @RequestBody GrantRequest req) {
        if (req.role()==PermissionRole.ADMIN && !auth.getName().equals("admin")) {
            throw new org.springframework.security.access.AccessDeniedException("Only the designated bootstrap admin may grant ADMIN.");
        }
        PermissionGrant g=new PermissionGrant(); g.setUsername(req.username()); g.setRole(req.role());
        g.setClientCode(defaultScope(req.clientCode())); g.setBranchCode(defaultScope(req.branchCode()));
        g.setDirection(defaultScope(req.direction())); g.setMode(defaultScope(req.mode()));
        g.setGrantedBy(auth.getName()); return grants.save(g);
    }
    @PostMapping("/admin/users/{username}/absence") @PreAuthorize("hasRole('ADMIN')")
    public StaffUser absence(@PathVariable String username,@RequestParam boolean absent) {
        StaffUser u=users.findByUsername(username).orElseThrow(()->new IllegalArgumentException("User not found"));
        u.setAbsent(absent); return users.save(u);
    }
    private String defaultScope(String s){return s==null||s.isBlank()?"*":s.trim().toUpperCase();}
}
