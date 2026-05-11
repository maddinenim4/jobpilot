package com.jobpilot.backend.service;

import com.jobpilot.backend.config.EncryptionUtil;
import com.jobpilot.backend.config.JwtUtil;
import com.jobpilot.backend.dto.LoginRequest;
import com.jobpilot.backend.dto.RegisterRequest;
import com.jobpilot.backend.dto.AuthResponse;
import com.jobpilot.backend.dto.UserSettingsRequest;
import com.jobpilot.backend.model.User;
import com.jobpilot.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EncryptionUtil encryptionUtil;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EncryptionUtil encryptionUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.encryptionUtil = encryptionUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already registered: " + request.getUsername());
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmailSignature(request.getEmailSignature());

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getUsername());

        return new AuthResponse(token, saved.getUsername(), saved.getFullName());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getFullName());
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public User updateSettings(String username, UserSettingsRequest request) {
        User user = getUserByUsername(username);

        if (request.getGmailApiKey() != null) {
            user.setGmailApiKey(encryptionUtil.encrypt(request.getGmailApiKey()));
        }
        if (request.getAiApiKey() != null) {
            user.setAiApiKey(encryptionUtil.encrypt(request.getAiApiKey()));
        }
        if (request.getAiModelType() != null) {
            user.setAiModelType(request.getAiModelType());
        }
        if (request.getEmailSignature() != null) {
            user.setEmailSignature(request.getEmailSignature());
        }
        if (request.getSchedulerEnabled() != null) {
            user.setSchedulerEnabled(request.getSchedulerEnabled());
        }
        if (request.getSchedulerIntervalHours() != null) {
            user.setSchedulerIntervalHours(request.getSchedulerIntervalHours());
        }
        if (request.getSchedulerStartTime() != null) {
            user.setSchedulerStartTime(request.getSchedulerStartTime());
        }
        if (request.getSchedulerEndTime() != null) {
            user.setSchedulerEndTime(request.getSchedulerEndTime());
        }
        if (request.getSchedulerKeywords() != null) {
            user.setSchedulerKeywords(request.getSchedulerKeywords());
        }
        if (request.getSchedulerPortals() != null) {
            user.setSchedulerPortals(request.getSchedulerPortals());
        }
        if (request.getSchedulerTitleFilter() != null) {
            user.setSchedulerTitleFilter(request.getSchedulerTitleFilter());
        }

        return userRepository.save(user);
    }

    public String getDecryptedGmailPassword(String username) {
        User user = getUserByUsername(username);
        if (user.getGmailApiKey() == null) {
            throw new RuntimeException("Gmail app password not configured");
        }
        return encryptionUtil.decrypt(user.getGmailApiKey());
    }

    public String getDecryptedAiApiKey(String username) {
        User user = getUserByUsername(username);
        if (user.getAiApiKey() == null) {
            throw new RuntimeException("AI API key not configured");
        }
        return encryptionUtil.decrypt(user.getAiApiKey());
    }
}
