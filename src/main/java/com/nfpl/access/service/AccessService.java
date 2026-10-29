package com.nfpl.access.service;

import com.nfpl.access.domain.*;
import com.nfpl.access.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccessService {
    private final PermissionGrantRepository grants;
    private final StaffUserRepository users;
    private final AuditEventRepository audits;

    public AccessService(PermissionGrantRepository grants, StaffUserRepository users, AuditEventRepository audits) {
        this.grants=grants; this.users=users; this.audits=audits;
    }

    @Transactional
    public AccessDecision check(String username, JobRecord job, String action) {
        StaffUser user = users.findByUsername(username).orElse(null);
        if (user == null || !user.isActive()) return deny(username, job, action, "INACTIVE_USER", "User is inactive or unknown.");
        if (user.isAbsent() && !action.equals("VIEW") && !action.equals("LIST")) {
            return deny(username, job, action, "USER_ABSENT", "User is marked absent; reassign or delegate the work.");
        }
        List<PermissionGrant> own = grants.findByUsernameAndActiveTrue(username);
        boolean admin = own.stream().anyMatch(g -> g.getRole()==PermissionRole.ADMIN);
        if (admin && !action.equals("CANCEL") && !action.equals("REOPEN")) {
            return allow(username, job, action, "ADMIN_ALLOWED", "Administrator permission applies.");
        }
        if (job.isArchived()) return deny(username, job, action, "JOB_ARCHIVED", "Archived jobs are read-only.");
        if (action.equals("VIEW") || action.equals("LIST")) {
            boolean visible = own.stream().anyMatch(g -> matches(g,job) &&
                (g.getRole()==PermissionRole.VIEWER || g.getRole()==PermissionRole.MAKER ||
                 g.getRole()==PermissionRole.CHECKER || g.getRole()==PermissionRole.CLIENT_VIEWER ||
                 g.getRole()==PermissionRole.PARSER));
            if (visible || job.getOwnerUsername().equals(username))
                return allow(username,job,action,"SCOPE_MATCH","A permission or ownership rule grants visibility.");
            return deny(username,job,action,"NO_VIEW_SCOPE","No active grant covers this job.");
        }
        if (action.equals("PREPARE")) {
            if (job.getStage()!=JobStage.CONFIRMED) return deny(username,job,action,"INVALID_STAGE","Only Confirmed jobs can be prepared.");
            if (has(own,PermissionRole.MAKER,job)) return allow(username,job,action,"MAKER_SCOPE_MATCH","Maker grant matches the job.");
            return deny(username,job,action,"NO_MAKER_SCOPE","No Maker grant covers this job.");
        }
        if (action.equals("CHECK")) {
            if (job.getStage()!=JobStage.PREPARED) return deny(username,job,action,"INVALID_STAGE","Only Prepared jobs can be checked.");
            if (job.getOwnerUsername().equals(username)) return deny(username,job,action,"MAKER_CHECKER_CONFLICT","The preparer cannot check their own job.");
            if (has(own,PermissionRole.CHECKER,job)) return allow(username,job,action,"CHECKER_SCOPE_MATCH","Checker grant matches the job.");
            return deny(username,job,action,"NO_CHECKER_SCOPE","No Checker grant covers this job.");
        }
        if (action.equals("FILE")) {
            if (job.getStage()!=JobStage.CHECKED) return deny(username,job,action,"INVALID_STAGE","Only Checked jobs can be filed.");
            if (has(own,PermissionRole.CHECKER,job) || admin) return allow(username,job,action,"FILING_AUTHORIZED","Checker or administrator may file this checked job.");
            return deny(username,job,action,"NO_FILE_PERMISSION","A Checker grant is required to file.");
        }
        if (action.equals("CANCEL")) {
            if (job.getStage()==JobStage.CANCELLED) return deny(username,job,action,"ALREADY_CANCELLED","Job is already cancelled.");
            if (admin) return allow(username,job,action,"ADMIN_CANCEL","Administrator may cancel; reason must be supplied in production.");
            if (job.getStage()!=JobStage.FILED && has(own,PermissionRole.MAKER,job))
                return allow(username,job,action,"MAKER_CANCEL","Maker may cancel a job before filing.");
            return deny(username,job,action,"CANCEL_REQUIRES_ADMIN","Cancelling a filed job requires an administrator.");
        }
        if (action.equals("REOPEN")) {
            if (job.getStage()!=JobStage.FILED && job.getStage()!=JobStage.CANCELLED)
                return deny(username,job,action,"NOT_CLOSED","Only Filed or Cancelled jobs can be reopened.");
            if (admin) return allow(username,job,action,"ADMIN_REOPEN","Administrator may reopen; reason must be supplied in production.");
            return deny(username,job,action,"REOPEN_REQUIRES_ADMIN","Reopening requires an administrator.");
        }
        if (action.equals("UPDATE")) {
            if (job.getStage()==JobStage.FILED) return deny(username,job,action,"FILED_READ_ONLY","Filed jobs cannot be edited through the ordinary update action.");
            if (has(own,PermissionRole.MAKER,job)) return allow(username,job,action,"MAKER_SCOPE_MATCH","Maker grant matches the job.");
            return deny(username,job,action,"NO_UPDATE_PERMISSION","No Maker grant covers this job.");
        }
        return deny(username,job,action,"UNKNOWN_ACTION","Action is not supported.");
    }

    private boolean has(List<PermissionGrant> grants, PermissionRole role, JobRecord job) {
        return grants.stream().anyMatch(g -> g.getRole()==role && matches(g,job));
    }
    private boolean matches(PermissionGrant g, JobRecord j) {
        return scope(g.getClientCode(),j.getClientCode()) && scope(g.getBranchCode(),j.getBranchCode())
            && scope(g.getDirection(),j.getDirection().name()) && scope(g.getMode(),j.getMode().name());
    }
    private boolean scope(String grant, String value) { return grant.equals("*") || grant.equalsIgnoreCase(value); }

    private AccessDecision allow(String u, JobRecord j, String a, String code, String message) {
        audit(u,j,a,true,code+": "+message); return new AccessDecision(true,code,message);
    }
    private AccessDecision deny(String u, JobRecord j, String a, String code, String message) {
        audit(u,j,a,false,code+": "+message); return new AccessDecision(false,code,message);
    }
    private void audit(String u, JobRecord j, String a, boolean allowed, String reason) {
        AuditEvent e=new AuditEvent(); e.setActorUsername(u); e.setJobId(j==null?null:j.getId());
        e.setAction(a); e.setAllowed(allowed); e.setReason(reason); audits.save(e);
    }
}
