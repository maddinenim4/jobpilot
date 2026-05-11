package com.jobpilot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String phone;

    @Column(length = 2000)
    private String emailSignature;

    @Column(length = 2000)
    private String gmailApiKey;

    private String aiModelType;

    @Column(length = 2000)
    private String aiApiKey;

    private Boolean schedulerEnabled = false;
    private Integer schedulerIntervalHours = 2;
    private String schedulerStartTime;
    private String schedulerEndTime;
    private String schedulerKeywords;
    private String schedulerPortals;
    private String schedulerTitleFilter;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
