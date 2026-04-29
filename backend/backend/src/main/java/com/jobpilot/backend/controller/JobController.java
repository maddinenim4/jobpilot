package com.jobpilot.backend.controller;

import com.jobpilot.backend.dto.*;
import com.jobpilot.backend.model.AppliedJob;
import com.jobpilot.backend.model.JobPosting;
import com.jobpilot.backend.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:5173")
public class JobController {

    private final JobService jobService;
    private final com.jobpilot.backend.service.AiService aiService;
    private final com.jobpilot.backend.service.UserService userService;
    private final com.jobpilot.backend.config.EncryptionUtil encryptionUtil;
    public JobController(JobService jobService,
                         com.jobpilot.backend.service.AiService aiService,
                         com.jobpilot.backend.service.UserService userService,
                         com.jobpilot.backend.config.EncryptionUtil encryptionUtil) {
        this.jobService = jobService;
        this.aiService = aiService;
        this.userService = userService;
        this.encryptionUtil = encryptionUtil;
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse> scanAndApply(Authentication authentication,
                                                     @RequestBody JobScanRequest request) {
        try {
            String username = authentication.getName();
            System.out.println("=== SCAN STARTED for user: " + username + " ===");
            JobScanResult result = new JobScanResult();
            List<JobResultItem> resultItems = new ArrayList<>();

            int totalFound = 0;
            int applied = 0;
            int skipped = 0;
            int failed = 0;

            List<JobPosting> allJobs = new ArrayList<>();
            List<String> portals = request.getPortals();
            if (portals == null || portals.isEmpty()) {
                portals = List.of("Nvoids");
            }
            String titleFilter = request.getTitleFilter();
            for (String keyword : request.getKeywords()) {
                List<JobPosting> found = jobService.scrapeSelectedPortals(portals, keyword, null);

                // STRICT title filter — every word in title filter must appear in job title
                if (titleFilter != null && !titleFilter.isBlank()) {
                    String[] titleWords = titleFilter.toLowerCase().trim().split("\\s+");
                    found = found.stream()
                            .filter(j -> {
                                if (j.getJobTitle() == null) return false;
                                String titleLower = j.getJobTitle().toLowerCase();
                                for (String word : titleWords) {
                                    if (!titleLower.contains(word)) return false;
                                }
                                return true;
                            })
                            .toList();
                    System.out.println("After title filter '" + titleFilter + "': " + found.size() + " jobs remain");
                }

                // Filter by posted time window
                Integer hours = request.getPostedWithinHours();
                if (hours != null && hours > 0) {
                    found = found.stream()
                            .filter(j -> isWithinHours(j.getPostedDate(), hours))
                            .toList();
                    System.out.println("After time filter (last " + hours + "h): " + found.size() + " jobs remain");
                }

                allJobs.addAll(found);
            }

            totalFound = allJobs.size();

            for (JobPosting job : allJobs) {
                JobResultItem item = new JobResultItem();
                item.setJobTitle(job.getJobTitle());
                item.setCompany(job.getCompany());
                item.setLocation(job.getLocation());
                item.setRecruiterEmail(job.getRecruiterEmail());
                item.setSourceUrl(job.getSourceUrl());
                item.setSourceSite(job.getSourceSite());

                try {
                    if (job.getRecruiterEmail() == null || job.getRecruiterEmail().isBlank()) {
                        item.setStatus("SKIPPED - No recruiter email");
                        skipped++;
                    } else {
                        AppliedJob appliedJob = jobService.applyToJob(username, job);
                        item.setResumeUsed(appliedJob.getResumeUsed());
                        item.setStatus("APPLIED");
                        applied++;
                    }
                } catch (RuntimeException e) {
                    if (e.getMessage().contains("Already applied")) {
                        item.setStatus("SKIPPED - Already applied");
                        skipped++;
                    } else {
                        item.setStatus("FAILED - " + e.getMessage());
                        failed++;
                    }
                }

                resultItems.add(item);
            }

            result.setTotalFound(totalFound);
            result.setMatched(totalFound - skipped);
            result.setApplied(applied);
            result.setSkipped(skipped);
            result.setFailed(failed);
            result.setResults(resultItems);

            return ResponseEntity.ok(new ApiResponse(true, "Scan completed", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/scrape/{portal}")
    public ResponseEntity<ApiResponse> scrapePortal(@PathVariable String portal,
                                                     @RequestParam String keyword,
                                                     @RequestParam(required = false) String location) {
        try {
            List<JobPosting> jobs = jobService.scrapeJobs(portal, keyword, location);
            return ResponseEntity.ok(new ApiResponse(true, "Found " + jobs.size() + " jobs", jobs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/applied")
    public ResponseEntity<ApiResponse> getAppliedJobs(Authentication authentication) {
        try {
            List<AppliedJob> jobs = jobService.getAppliedJobs(authentication.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Applied jobs retrieved", jobs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            long totalApplied = jobService.getAppliedCount(username, "APPLIED");
            var stats = new java.util.HashMap<String, Object>();
            stats.put("totalApplied", totalApplied);
            return ResponseEntity.ok(new ApiResponse(true, "Stats retrieved", stats));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/portals")
    public ResponseEntity<ApiResponse> getAvailablePortals() {
        return ResponseEntity.ok(new ApiResponse(true, "Portals retrieved",
                com.jobpilot.backend.scraper.PortalConfig.getAllPortals().keySet()));
    }

    @PostMapping("/quick-apply")
    public ResponseEntity<ApiResponse> quickApply(Authentication authentication,
                                                   @RequestBody java.util.Map<String, String> request) {
        try {
            String username = authentication.getName();
            String jobDescription = request.get("jobDescription");
            String recruiterEmail = request.get("recruiterEmail");
            String jobTitle = request.get("jobTitle");
            String company = request.get("company");
            String location = request.get("location");

            if (jobDescription == null || jobDescription.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Job description is required", null));
            }
            if (recruiterEmail == null || recruiterEmail.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Recruiter email is required", null));
            }

            // Auto-extract title and location from JD using AI
            com.jobpilot.backend.model.User currentUser = userService.getUserByUsername(username);
            String aiKey = encryptionUtil.decrypt(currentUser.getAiApiKey());
            String aiModel = currentUser.getAiModelType();
            String extractedTitle = "Position";
            String extractedLocation = "Remote";
            try {
                extractedTitle = aiService.extractJobTitleFromJD(jobDescription, aiKey, aiModel);
                extractedLocation = aiService.extractLocationFromJD(jobDescription, aiKey, aiModel);
                System.out.println("Quick Apply extracted — Title: " + extractedTitle + ", Location: " + extractedLocation);
            } catch (Exception e) {
                System.err.println("Failed to extract title/location: " + e.getMessage());
            }

            JobPosting job = JobPosting.builder()
                    .jobTitle(extractedTitle)
                    .company(company != null && !company.isBlank() ? company : "Company")
                    .location(extractedLocation)
                    .jobDescription(jobDescription)
                    .recruiterEmail(recruiterEmail)
                    .sourceSite("Manual")
                    .sourceUrl("")
                    .postedDate("")
                    .build();

            AppliedJob appliedJob = jobService.applyToJob(username, job);

            JobResultItem item = new JobResultItem();
            item.setJobTitle(appliedJob.getJobTitle());
            item.setCompany(appliedJob.getCompany());
            item.setLocation(appliedJob.getLocation());
            item.setRecruiterEmail(appliedJob.getRecruiterEmail());
            item.setResumeUsed(appliedJob.getResumeUsed());
            item.setStatus("APPLIED");

            return ResponseEntity.ok(new ApiResponse(true, "Application sent successfully!", item));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    private boolean isWithinHours(String postedDate, int hours) {
        if (postedDate == null || postedDate.isBlank()) return true;
        try {
            // Nvoids format: "01:04 AM 07-Apr-26"
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a dd-MMM-yy", java.util.Locale.ENGLISH);
            java.time.LocalDateTime posted = java.time.LocalDateTime.parse(postedDate.trim(), fmt);
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(hours);
            return posted.isAfter(cutoff);
        } catch (Exception e) {
            return true; // if parsing fails, don't filter it out
        }
    }
}
