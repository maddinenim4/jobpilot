package com.jobpilot.backend.dto;

import lombok.Data;

@Data
public class UserSettingsRequest {
    private String gmailApiKey;
    private String aiApiKey;
    private String aiModelType;
    private String emailSignature;
    private Boolean schedulerEnabled;
    private Integer schedulerIntervalHours;
    private String schedulerStartTime;
    private String schedulerEndTime;
    private String schedulerKeywords;
    private String schedulerPortals;
    private String schedulerTitleFilter;
}
