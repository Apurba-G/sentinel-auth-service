package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.request.*;
import com.ecommerce.auth.dto.response.*;
import com.ecommerce.auth.entity.SecurityQuestion;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.auth.service.CaptchaService;
import com.ecommerce.auth.service.SecurityVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "SentinelAuth Core", description = "Universal Zero-Trust Authentication, Captcha, Dynamic Signup & Recovery Endpoints")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final SecurityVerificationService verificationService;

    @GetMapping("/captcha")
    @Operation(summary = "Generate CAPTCHA Challenge", description = "Returns a base64-encoded distorted PNG image and unique challenge ID valid for 3 minutes.")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateCaptcha() {
        Map<String, String> captchaData = captchaService.generateCaptcha();
        return ResponseEntity.ok(ApiResponse.success("CAPTCHA generated successfully", captchaData));
    }

    @GetMapping("/security-questions")
    @Operation(summary = "List Active Security Questions", description = "Returns active security questions for user registration and identity verification.")
    public ResponseEntity<ApiResponse<List<SecurityQuestion>>> getSecurityQuestions() {
        List<SecurityQuestion> questions = verificationService.getActiveSecurityQuestions();
        return ResponseEntity.ok(ApiResponse.success("Active security questions retrieved", questions));
    }

    @PostMapping("/register")
    @Operation(summary = "Register User with Dynamic Fields & Security Keys", description = "Creates account, registers custom project dynamic fields, answers, and returns master 12-word recovery mnemonic.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully. Store your master recovery phrase safely.", response));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login with Captcha & 4-Attempt / 72-Hour Defense", description = "Authenticates user credentials. Exceeding 4 attempts enforces a strict 72-hour lockout.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Generate Temporary Password via Recovery Key", description = "Validates 12-word mnemonic phrase, backup code, or security answer to issue a 24-hour temporary password.")
    public ResponseEntity<ApiResponse<TemporaryPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        TemporaryPasswordResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Temporary password generated successfully", response));
    }

    @PostMapping("/check-password")
    @Operation(summary = "Check Password Strength & RedisBloom History", description = "Evaluates entropy and verifies against RedisBloom breached databases and repetition history.")
    public ResponseEntity<ApiResponse<PasswordCheckResponse>> checkPassword(
            @Valid @RequestBody PasswordCheckRequest request,
            @RequestParam(required = false) String email
    ) {
        PasswordCheckResponse response = authService.checkPassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Password analysis completed", response));
    }

    @PostMapping("/unlock/trigger-otp")
    @Operation(summary = "Google-Style Unlock: Trigger Verification OTP", description = "Sends a 6-digit verification code to email or phone to release an account locked by 72-hour lockout.")
    public ResponseEntity<ApiResponse<String>> triggerUnlockOtp(
            @RequestParam String identifier,
            @RequestParam(defaultValue = "EMAIL_OTP") String challengeType,
            HttpServletRequest servletRequest
    ) {
        String ip = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        String otp = authService.triggerAccountUnlockOtp(identifier, challengeType, ip, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Verification challenge code sent", otp));
    }

    @PostMapping("/unlock/verify")
    @Operation(summary = "Google-Style Unlock: Verify & Release Lockout", description = "Verifies 6-digit challenge code or security answer to restore account access.")
    public ResponseEntity<ApiResponse<String>> verifyAndUnlock(
            @Valid @RequestBody UnlockAccountRequest request
    ) {
        boolean unlocked = authService.unlockAccount(request);
        if (unlocked) {
            return ResponseEntity.ok(ApiResponse.success("Account unlocked successfully. You may now log in.", "UNLOCKED"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Security challenge verification failed."));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
