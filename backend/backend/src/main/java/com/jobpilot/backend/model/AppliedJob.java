package com.jobpilot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "applied_jobs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "recruiter_email", "job_title", "location"})
})
public class AppliedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "job_title")
    private String jobTitle;

    private String company;

    @Column(name = "location")
    private String location;

    @Column(name = "recruiter_email")
    private String recruiterEmail;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(length = 1000)
    private String sourceUrl;

    private String sourceSite;
    private String jobPostId;
    private String postedDate;
    private String resumeUsed;
    private String emailSubject;

    @Column(columnDefinition = "TEXT")
    private String emailBody;

    private String status;
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
    }
}
