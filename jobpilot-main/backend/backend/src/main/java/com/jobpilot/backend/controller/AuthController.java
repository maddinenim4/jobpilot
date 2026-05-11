package com.jobpilot.backend.controller;

import com.jobpilot.backend.dto.ApiResponse;
import com.jobpilot.backend.dto.AuthResponse;
import com.jobpilot.backend.dto.LoginRequest;
import com.jobpilot.backend.dto.RegisterRequest;
import com.jobpilot.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse auth = userService.register(request);
            return ResponseEntity.ok(new ApiResponse(true, "Registration successful", auth));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse auth = userService.login(request);
            return ResponseEntity.ok(new ApiResponse(true, "Login successful", auth));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}
