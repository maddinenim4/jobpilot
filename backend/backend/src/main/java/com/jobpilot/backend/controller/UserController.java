package com.jobpilot.backend.controller;

import com.jobpilot.backend.dto.ApiResponse;
import com.jobpilot.backend.dto.UserSettingsRequest;
import com.jobpilot.backend.model.User;
import com.jobpilot.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getProfile(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            user.setPassword(null);
            user.setGmailApiKey(user.getGmailApiKey() != null ? "****" : null);
            user.setAiApiKey(user.getAiApiKey() != null ? "****" : null);
            return ResponseEntity.ok(new ApiResponse(true, "Profile retrieved", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse> updateSettings(Authentication authentication,
                                                       @RequestBody UserSettingsRequest request) {
        try {
            User updated = userService.updateSettings(authentication.getName(), request);
            updated.setPassword(null);
            updated.setGmailApiKey(updated.getGmailApiKey() != null ? "****" : null);
            updated.setAiApiKey(updated.getAiApiKey() != null ? "****" : null);
            return ResponseEntity.ok(new ApiResponse(true, "Settings updated", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}
