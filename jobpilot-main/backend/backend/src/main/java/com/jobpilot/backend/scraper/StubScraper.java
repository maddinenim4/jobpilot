package com.jobpilot.backend.scraper;

import com.jobpilot.backend.model.JobPosting;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StubScraper implements JobScraper {

    @Override
    public String getPortalName() {
        return "Stub";
    }

    @Override
    public List<JobPosting> scrapeJobs(String keyword, String location) {
        List<JobPosting> jobs = new ArrayList<>();

        jobs.add(JobPosting.builder()
                .jobTitle(".NET Developer")
                .company("Acme Corp")
                .location(location != null ? location : "Remote")
                .jobDescription("Seeking a .NET developer with 5+ years experience in C#, ASP.NET Core, Azure, and SQL Server.")
                .recruiterEmail("recruiter@acmecorp.example.com")
                .sourceSite("Stub")
                .sourceUrl("https://example.com/jobs/dotnet-dev-1")
                .postedDate("2026-04-01")
                .build());

        jobs.add(JobPosting.builder()
                .jobTitle("Senior .NET Engineer")
                .company("TechFlow Solutions")
                .location("Boston, MA")
                .jobDescription("Senior .NET engineer needed. Must have experience with microservices, Docker, Kubernetes, and CI/CD pipelines.")
                .recruiterEmail("hiring@techflow.example.com")
                .sourceSite("Stub")
                .sourceUrl("https://example.com/jobs/sr-dotnet-2")
                .postedDate("2026-04-01")
                .build());

        jobs.add(JobPosting.builder()
                .jobTitle("Full Stack .NET Developer")
                .company("Insight Global")
                .location("New York, NY")
                .jobDescription("Full stack role using .NET 8, Blazor, React, and SQL Server. Healthcare domain experience preferred.")
                .recruiterEmail("jobs@insightglobal.example.com")
                .sourceSite("Stub")
                .sourceUrl("https://example.com/jobs/fs-dotnet-3")
                .postedDate("2026-04-01")
                .build());

        return jobs;
    }
}
