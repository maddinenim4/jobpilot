package com.jobpilot.backend.controller;

import com.jobpilot.backend.dto.ApiResponse;
import com.jobpilot.backend.model.Resume;
import com.jobpilot.backend.model.User;
import com.jobpilot.backend.service.ResumeService;
import com.jobpilot.backend.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    public ResumeController(ResumeService resumeService, UserService userService) {
        this.resumeService = resumeService;
        this.userService = userService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> uploadResume(Authentication authentication,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam("label") String label) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            Resume resume = resumeService.uploadResume(user.getId(), label, file);
            resume.setFileData(null);
            return ResponseEntity.ok(new ApiResponse(true, "Resume uploaded successfully", resume));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllResumes(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            List<Resume> resumes = resumeService.getResumesByUser(user.getId());
            resumes.forEach(r -> r.setFileData(null));
            return ResponseEntity.ok(new ApiResponse(true, "Resumes retrieved", resumes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadResume(Authentication authentication,
                                                  @PathVariable Long id) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            Resume resume = resumeService.getResumeById(id, user.getId());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resume.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resume.getFileName() + "\"")
                    .body(resume.getFileData());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/label")
    public ResponseEntity<ApiResponse> updateLabel(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @RequestBody String newLabel) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            Resume updated = resumeService.updateLabel(id, user.getId(), newLabel);
            updated.setFileData(null);
            return ResponseEntity.ok(new ApiResponse(true, "Label updated", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteResume(Authentication authentication,
                                                     @PathVariable Long id) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            resumeService.deleteResume(id, user.getId());
            return ResponseEntity.ok(new ApiResponse(true, "Resume deleted", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}
