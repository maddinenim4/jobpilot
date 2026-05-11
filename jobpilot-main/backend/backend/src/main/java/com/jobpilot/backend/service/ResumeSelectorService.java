package com.jobpilot.backend.service;

import com.jobpilot.backend.model.Resume;
import com.jobpilot.backend.repository.ResumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ResumeSelectorService {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private AtsScoreService atsScoreService;

    public Optional<Resume> selectBestResume(String jobDescription) {
        List<Resume> resumes = resumeRepository.findByIsActiveTrue();

        if (resumes.isEmpty()) {
            System.out.println("ResumeSelectorService: No resumes in DB.");
            return Optional.empty();
        }

        Resume bestResume = null;
        double bestScore = -1;

        for (Resume resume : resumes) {
            double score = atsScoreService.calculateScore(
                jobDescription, resume.getParsedText()
            );
            System.out.printf("ATS Score [%s]: %.2f%%%n",
                resume.getFileName(), score);

            if (score > bestScore) {
                bestScore = score;
                bestResume = resume;
            }
        }

        System.out.printf("Selected Resume: %s (ATS Score: %.2f%%)%n",
                bestResume != null ? bestResume.getFileName() : "None",
                bestScore);

        return Optional.ofNullable(bestResume);
    }
}
