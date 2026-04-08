package com.jobpilot.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobScanResult {
    private int totalFound;
    private int matched;
    private int applied;
    private int skipped;
    private int failed;
    private List<JobResultItem> results;
}
