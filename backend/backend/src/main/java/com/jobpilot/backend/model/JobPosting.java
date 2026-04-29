package com.jobpilot.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    private String jobTitle;
    private String company;
    private String location;
    private String recruiterEmail;
    private String jobDescription;
    private String sourceUrl;
    private String sourceSite;
    private String jobPostId;
    private String postedDate;
}
