package com.jobpilot.backend.dto;

import lombok.Data;

@Data
public class JobResultItem {
    private String jobTitle;
    private String company;
    private String location;
    private String recruiterEmail;
    private String resumeUsed;
    private String status;
    private String sourceUrl;
    private String sourceSite;
}
