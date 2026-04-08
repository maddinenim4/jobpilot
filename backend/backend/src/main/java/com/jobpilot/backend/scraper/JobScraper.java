package com.jobpilot.backend.scraper;

import com.jobpilot.backend.model.JobPosting;

import java.util.List;

public interface JobScraper {

    String getPortalName();

    List<JobPosting> scrapeJobs(String keyword, String location);
}
