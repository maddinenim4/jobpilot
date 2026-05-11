package com.jobpilot.backend.controller;

import com.jobpilot.backend.model.Resume;
import com.jobpilot.backend.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadResume(
            @RequestParam("file") MultipartFile file) {
        try {
            resumeService.saveResume(file);
            return ResponseEntity.ok("Resume uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Resume>> getAllResumes() {
        return ResponseEntity.ok(resumeService.getAllResumes());
    }
}
