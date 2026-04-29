package com.jobpilot.backend.repository;

import com.jobpilot.backend.model.AppliedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppliedJobRepository extends JpaRepository<AppliedJob, Long> {
    List<AppliedJob> findByUserIdOrderByAppliedAtDesc(Long userId);
    boolean existsByUserIdAndRecruiterEmailAndJobTitleAndLocation(
        Long userId, String recruiterEmail, String jobTitle, String location);
    long countByUserIdAndStatus(Long userId, String status);
}
