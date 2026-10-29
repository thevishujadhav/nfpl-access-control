package com.nfpl.access.ui;

import com.nfpl.access.domain.*;
import com.nfpl.access.repo.AuditEventRepository;
import com.nfpl.access.repo.JobRepository;
import com.nfpl.access.repo.PermissionGrantRepository;
import com.nfpl.access.repo.StaffUserRepository;
import com.nfpl.access.service.AccessDecision;
import com.nfpl.access.service.AccessService;
import com.nfpl.access.service.JobService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
@Validated
public class UiController {
    private final JobRepository jobs;
    private final StaffUserRepository users;
    private final PermissionGrantRepository grants;
    private final AuditEventRepository audits;
    private final AccessService accessService;
    private final JobService jobService;

    public UiController(JobRepository jobs, StaffUserRepository users, PermissionGrantRepository grants,
                        AuditEventRepository audits, AccessService accessService, JobService jobService) {
        this.jobs = jobs;
        this.users = users;
        this.grants = grants;
        this.audits = audits;
        this.accessService = accessService;
        this.jobService = jobService;
    }

    @ModelAttribute
    public void addCommonModel(Model model, Authentication auth) {
        if (auth != null) {
            model.addAttribute("currentUsername", auth.getName());
            model.addAttribute("isAdmin", isAdmin(auth.getName()));
        }
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        StaffUser user = users.findByUsername(auth.getName()).orElseThrow();
        Page<JobRecord> page = jobService.list(auth.getName(), PageRequest.of(0, 100, Sort.by("id").descending()));
        List<JobRecord> visibleJobs = page.getContent();
        model.addAttribute("user", user);
        model.addAttribute("jobs", visibleJobs.stream().limit(8).toList());
        model.addAttribute("visibleCount", visibleJobs.size());
        model.addAttribute("confirmedCount", visibleJobs.stream().filter(j -> j.getStage() == JobStage.CONFIRMED).count());
        model.addAttribute("preparedCount", visibleJobs.stream().filter(j -> j.getStage() == JobStage.PREPARED).count());
        model.addAttribute("checkedCount", visibleJobs.stream().filter(j -> j.getStage() == JobStage.CHECKED).count());
        model.addAttribute("filedCount", visibleJobs.stream().filter(j -> j.getStage() == JobStage.FILED).count());
        model.addAttribute("isAdmin", isAdmin(auth.getName()));
        return "dashboard";
    }

    @GetMapping("/jobs")
    public String jobs(Authentication auth, Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<JobRecord> result = jobService.list(auth.getName(), PageRequest.of(safePage, safeSize, Sort.by("id").descending()));
        model.addAttribute("jobPage", result);
        model.addAttribute("jobs", result.getContent());
        model.addAttribute("isAdmin", isAdmin(auth.getName()));
        return "jobs";
    }

    @PostMapping("/ui/jobs/{id}/transition")
    public String transition(Authentication auth, @PathVariable Long id,
                             @RequestParam @NotBlank String action,
                             @RequestParam(required = false) String reason,
                             RedirectAttributes flash) {
        try {
            jobService.transition(auth.getName(), id, action.trim().toUpperCase(Locale.ROOT), reason);
            flash.addFlashAttribute("successMessage", "Job #" + id + " updated successfully.");
        } catch (AccessDeniedException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs";
    }

    @GetMapping("/access-check")
    public String accessCheck(Authentication auth, Model model) {
        model.addAttribute("jobs", jobService.list(auth.getName(), PageRequest.of(0, 100, Sort.by("id").descending())).getContent());
        model.addAttribute("actions", List.of("VIEW", "PREPARE", "CHECK", "FILE", "CANCEL", "REOPEN", "UPDATE"));
        model.addAttribute("isAdmin", isAdmin(auth.getName()));
        return "access-check";
    }

    @PostMapping("/ui/access-check")
    public String checkAccess(Authentication auth, @RequestParam Long jobId, @RequestParam String action,
                              RedirectAttributes flash) {
        try {
            JobRecord job = jobs.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found."));
            AccessDecision decision = accessService.check(auth.getName(), job, action.trim().toUpperCase(Locale.ROOT));
            flash.addFlashAttribute("decisionAllowed", decision.allowed());
            flash.addFlashAttribute("decisionCode", decision.reasonCode());
            flash.addFlashAttribute("decisionMessage", decision.message());
            flash.addFlashAttribute("decisionJob", "#" + job.getId() + " · " + job.getTitle());
            flash.addFlashAttribute("decisionAction", action.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/access-check";
    }

    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public String audit(Model model, @RequestParam(required = false) Long jobId,
                        @RequestParam(defaultValue = "0") int page) {
        Page<AuditEvent> auditPage = jobId == null
            ? audits.findAllByOrderByOccurredAtDesc(PageRequest.of(Math.max(page, 0), 50, Sort.by("occurredAt").descending()))
            : audits.findByJobIdOrderByOccurredAtDesc(jobId, PageRequest.of(Math.max(page, 0), 50, Sort.by("occurredAt").descending()));
        model.addAttribute("auditPage", auditPage);
        model.addAttribute("events", auditPage.getContent());
        model.addAttribute("jobId", jobId);
        model.addAttribute("isAdmin", true);
        return "audit";
    }

    @GetMapping("/admin/grants-ui")
    @PreAuthorize("hasRole('ADMIN')")
    public String grants(Model model) {
        model.addAttribute("grants", grants.findAll());
        model.addAttribute("users", users.findAll());
        model.addAttribute("isAdmin", true);
        return "grants";
    }

    @PostMapping("/admin/users/{username}/absence-ui")
    @PreAuthorize("hasRole('ADMIN')")
    public String absence(@PathVariable String username, @RequestParam boolean absent, RedirectAttributes flash) {
        StaffUser user = users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setAbsent(absent);
        users.save(user);
        flash.addFlashAttribute("successMessage", username + (absent ? " marked absent." : " marked present."));
        return "redirect:/admin/grants-ui";
    }

    private boolean isAdmin(String username) {
        return grants.findByUsernameAndActiveTrue(username).stream().anyMatch(g -> g.getRole() == PermissionRole.ADMIN);
    }
}
