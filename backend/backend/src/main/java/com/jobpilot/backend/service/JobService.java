package com.jobpilot.backend.service;

import com.jobpilot.backend.config.EncryptionUtil;
import com.jobpilot.backend.model.AppliedJob;
import com.jobpilot.backend.model.JobPosting;
import com.jobpilot.backend.model.Resume;
import com.jobpilot.backend.model.User;
import com.jobpilot.backend.repository.AppliedJobRepository;
import com.jobpilot.backend.scraper.GenericPortalScraper;
import com.jobpilot.backend.scraper.JobScraper;
import com.jobpilot.backend.scraper.PortalConfig;
import com.jobpilot.backend.scraper.ScraperFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobService {

    private final ScraperFactory scraperFactory;
    private final GenericPortalScraper genericPortalScraper;
    private final AiService aiService;
    private final EmailService emailService;
    private final ResumeService resumeService;
    private final UserService userService;
    private final AppliedJobRepository appliedJobRepository;
    private final EncryptionUtil encryptionUtil;

    public JobService(ScraperFactory scraperFactory, GenericPortalScraper genericPortalScraper,
                      AiService aiService, EmailService emailService, ResumeService resumeService,
                      UserService userService, AppliedJobRepository appliedJobRepository,
                      EncryptionUtil encryptionUtil) {
        this.scraperFactory = scraperFactory;
        this.genericPortalScraper = genericPortalScraper;
        this.aiService = aiService;
        this.emailService = emailService;
        this.resumeService = resumeService;
        this.userService = userService;
        this.appliedJobRepository = appliedJobRepository;
        this.encryptionUtil = encryptionUtil;
    }

    public List<JobPosting> scrapeJobs(String portalName, String keyword, String location) {
        JobScraper scraper = scraperFactory.getScraper(portalName);
        return scraper.scrapeJobs(keyword, location);
    }

    public List<JobPosting> scrapeSelectedPortals(List<String> portals, String keyword, String location) {
        List<JobPosting> allJobs = new ArrayList<>();

        for (String portal : portals) {
            try {
                System.out.println("=== Scraping portal: " + portal + " for keyword: " + keyword + " ===");

                if (scraperFactory.hasScraperFor(portal)) {
                    // Use dedicated scraper (Nvoids has its own)
                    allJobs.addAll(scraperFactory.getScraper(portal).scrapeJobs(keyword, location));
                } else if (PortalConfig.isPortal(portal)) {
                    // Use generic Selenium scraper
                    allJobs.addAll(genericPortalScraper.scrapePortal(portal, keyword));
                } else {
                    System.err.println("Unknown portal: " + portal);
                }
            } catch (Exception e) {
                System.err.println("Scraper failed for " + portal + ": " + e.getMessage());
            }
        }

        return allJobs;
    }

    public AppliedJob applyToJob(String username, JobPosting job) {
        User user = userService.getUserByUsername(username);

        if (appliedJobRepository.existsByUserIdAndRecruiterEmailAndJobTitleAndLocation(
                user.getId(), job.getRecruiterEmail(), job.getJobTitle(), job.getLocation())) {
            throw new RuntimeException("Already applied to this job: " + job.getJobTitle() + " at " + job.getCompany());
        }

        List<Resume> resumes = resumeService.getResumesByUser(user.getId());
        if (resumes.isEmpty()) {
            throw new RuntimeException("No resumes uploaded. Please upload at least one resume.");
        }

        String apiKey = encryptionUtil.decrypt(user.getAiApiKey());
        String aiProvider = user.getAiModelType();

        Resume bestResume = aiService.selectBestResume(resumes, job.getJobDescription(), apiKey, aiProvider);

        String emailSubject = aiService.generateEmailSubject(job.getJobTitle(), job.getCompany(), apiKey, aiProvider);
        String emailBody = aiService.generateEmailBody(
                job.getJobDescription(), bestResume.getExtractedText(),
                job.getRecruiterEmail(), apiKey, aiProvider);

        String gmailPassword = encryptionUtil.decrypt(user.getGmailApiKey());
        String senderEmail = user.getUsername();

        emailService.sendApplicationEmail(
                senderEmail, gmailPassword,
                job.getRecruiterEmail(), emailSubject,
                emailBody, bestResume,
                user.getEmailSignature());

        AppliedJob appliedJob = new AppliedJob();
        appliedJob.setUserId(user.getId());
        appliedJob.setJobTitle(job.getJobTitle());
        appliedJob.setCompany(job.getCompany());
        appliedJob.setLocation(job.getLocation());
        appliedJob.setRecruiterEmail(job.getRecruiterEmail());
        appliedJob.setJobDescription(job.getJobDescription());
        appliedJob.setSourceUrl(job.getSourceUrl());
        appliedJob.setSourceSite(job.getSourceSite());
        appliedJob.setJobPostId(job.getJobPostId());
        appliedJob.setPostedDate(job.getPostedDate());
        appliedJob.setResumeUsed(bestResume.getResumeLabel());
        appliedJob.setEmailSubject(emailSubject);
        appliedJob.setEmailBody(emailBody);
        appliedJob.setStatus("APPLIED");

        return appliedJobRepository.save(appliedJob);
    }

    public List<AppliedJob> getAppliedJobs(String username) {
        User user = userService.getUserByUsername(username);
        return appliedJobRepository.findByUserIdOrderByAppliedAtDesc(user.getId());
    }

    public long getAppliedCount(String username, String status) {
        User user = userService.getUserByUsername(username);
        return appliedJobRepository.countByUserIdAndStatus(user.getId(), status);
    }
}
