package com.jobpilot.backend.service;

import com.jobpilot.backend.model.JobPosting;
import com.jobpilot.backend.model.User;
import com.jobpilot.backend.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulerService {

    private final UserRepository userRepository;
    private final JobService jobService;

    // Tracks the last time each user's scheduled scan actually ran
    private final Map<Long, LocalDateTime> lastRunPerUser = new ConcurrentHashMap<>();

    public SchedulerService(UserRepository userRepository, JobService jobService) {
        this.userRepository = userRepository;
        this.jobService = jobService;
    }

    // Runs every 2 minutes to check if any user's scheduler should fire
    @Scheduled(fixedRate = 120000)
    public void checkSchedulers() {
        LocalTime now = LocalTime.now();
        LocalDateTime nowDateTime = LocalDateTime.now();
        System.out.println("=== Scheduler Check at " + now.format(DateTimeFormatter.ofPattern("HH:mm")) + " ===");

        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (!Boolean.TRUE.equals(user.getSchedulerEnabled())) continue;
            if (user.getSchedulerStartTime() == null || user.getSchedulerEndTime() == null) continue;
            if (user.getSchedulerKeywords() == null || user.getSchedulerKeywords().isBlank()) continue;

            try {
                LocalTime startTime = LocalTime.parse(user.getSchedulerStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime endTime = LocalTime.parse(user.getSchedulerEndTime(), DateTimeFormatter.ofPattern("HH:mm"));

                // Check if current time is within the scheduled window
                if (now.isBefore(startTime) || now.isAfter(endTime)) {
                    System.out.println("  User " + user.getUsername() + ": outside scheduled window (" + startTime + " - " + endTime + ")");
                    continue;
                }

                // Get user's interval (stored as minutes in schedulerIntervalHours field)
                int intervalMinutes = user.getSchedulerIntervalHours() != null ? user.getSchedulerIntervalHours() : 60;

                // Check if enough time has passed since last run
                LocalDateTime lastRun = lastRunPerUser.get(user.getId());
                if (lastRun != null) {
                    long minutesSinceLastRun = Duration.between(lastRun, nowDateTime).toMinutes();
                    if (minutesSinceLastRun < intervalMinutes) {
                        System.out.println("  User " + user.getUsername() + ": last ran " + minutesSinceLastRun + " min ago, waiting for " + intervalMinutes + " min interval");
                        continue;
                    }
                }

                // Mark as run BEFORE scanning (so a long scan doesn't cause double-fires)
                lastRunPerUser.put(user.getId(), nowDateTime);

                System.out.println("=== Running scheduled scan for user: " + user.getUsername() + " (interval: " + intervalMinutes + " min) ===");

                // Parse keywords
                List<String> keywords = Arrays.asList(user.getSchedulerKeywords().split(","));

                // Parse portals
                List<String> portals;
                if (user.getSchedulerPortals() != null && !user.getSchedulerPortals().isBlank()) {
                    portals = Arrays.asList(user.getSchedulerPortals().split(","));
                } else {
                    portals = List.of("Nvoids");
                }

                // Run scan for each keyword
                for (String keyword : keywords) {
                    String trimmedKeyword = keyword.trim();
                    if (trimmedKeyword.isEmpty()) continue;

                    try {
                        List<JobPosting> jobs = jobService.scrapeSelectedPortals(portals, trimmedKeyword, null);
                        System.out.println("  Scheduler found " + jobs.size() + " jobs for keyword: " + trimmedKeyword);

                        // Filter: only jobs posted AFTER the scheduler start time today
                        LocalDateTime todayStart = LocalDateTime.of(java.time.LocalDate.now(), startTime);
                        jobs = jobs.stream()
                                .filter(j -> isPostedAfter(j.getPostedDate(), todayStart))
                                .toList();
                        System.out.println("  After filtering old jobs: " + jobs.size() + " fresh jobs remain");

                        for (JobPosting job : jobs) {
                            if (job.getRecruiterEmail() == null || job.getRecruiterEmail().isBlank()) continue;

                            try {
                                jobService.applyToJob(user.getUsername(), job);
                                System.out.println("  Scheduler applied: " + job.getJobTitle());
                            } catch (RuntimeException e) {
                                if (!e.getMessage().contains("Already applied")) {
                                    System.err.println("  Scheduler apply failed: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("  Scheduler scan failed for " + trimmedKeyword + ": " + e.getMessage());
                    }
                }

                System.out.println("=== Scheduled scan completed for user: " + user.getUsername() + " ===");

            } catch (Exception e) {
                System.err.println("Scheduler error for user " + user.getUsername() + ": " + e.getMessage());
            }
        }
    }

    // Called when user turns scheduler off or changes settings — clears their last-run tracker
    public void resetUserSchedule(Long userId) {
        lastRunPerUser.remove(userId);
    }

    private boolean isPostedAfter(String postedDate, LocalDateTime cutoff) {
        if (postedDate == null || postedDate.isBlank()) return false;
        try {
            // Nvoids format: "01:04 AM 07-Apr-26"
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a dd-MMM-yy", java.util.Locale.ENGLISH);
            LocalDateTime posted = LocalDateTime.parse(postedDate.trim(), fmt);
            return posted.isAfter(cutoff);
        } catch (Exception e) {
            return false; // if we can't parse the date, skip it to be safe
        }
    }
}
