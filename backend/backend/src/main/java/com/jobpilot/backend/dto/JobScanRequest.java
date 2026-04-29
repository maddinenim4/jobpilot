package com.jobpilot.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobScanRequest {
    private List<String> keywords;
    private int timePeriodHours;
    private List<String> jobSiteUrls;
    private List<String> portals;
    private String titleFilter;
    private Integer postedWithinHours;
}
