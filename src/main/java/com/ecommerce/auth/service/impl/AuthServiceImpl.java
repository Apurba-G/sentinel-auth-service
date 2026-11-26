package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.dto.request.*;
import com.ecommerce.auth.dto.response.*;
import com.ecommerce.auth.entity.PasswordHistory;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.exception.AuthException;
import com.ecommerce.auth.repository.PasswordHistoryRepository;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;
    private final RedisBloomPasswordService bloomPasswordService;
    private final CaptchaService captchaService;
    private final RecoveryKeyService recoveryKeyService;
    private final TemporaryPasswordService tempPasswordService;
    private final SecurityVerificationService verificationService;
    private final BlockchainAuditService blockchainAuditService;

    @Value("${application.security.lockout.max-failed-attempts:4}")
    private int maxFailedAttempts;

    @Value("${application.security.lockout.lockout-duration-hours:72}")
    private int lockoutDurationHours;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email is already registered: " + request.getEmail());
        }
        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("Username is already taken: " + request.getUsername());
        }

        // 1. Password security validations
        if (bloomPasswordService.isCommonBreachedPassword(request.getPassword())) {
            throw new AuthException("The chosen password appears in known breached databases. Choose a stronger password.");
        }

        // 2. Resolve or assign default roles
        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new AuthException("Role not found: " + roleName));
                roles.add(role);
            }
        } else {
            Role defaultRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("ROLE_CUSTOMER")
                            .description("Default Customer Role")
                            .build()));
            roles.add(defaultRole);
        }

        // 3. Generate Master BIP-39 Mnemonic Phrase & Backup Codes
        String mnemonicPhrase = recoveryKeyService.generateMnemonicPhrase();
        List<String> backupCodes = recoveryKeyService.generateBackupCodes(5);

        // 4. Build User with Extensible Dynamic Attributes
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .roles(roles)
                .mustChangePassword(false)
                .failedLoginAttempts(0)
                .dynamicAttributes(request.getDynamicAttributes() != null ? request.getDynamicAttributes() : new HashMap<>())
                .build();

        User savedUser = userRepository.save(user);

        // 5. Store hashed recovery phrase & backup codes
        recoveryKeyService.saveRecoveryPhrase(savedUser, mnemonicPhrase);
        recoveryKeyService.saveBackupCodes(savedUser, backupCodes);

        // 6. Register Security Questions if provided
        if (request.getSecurityAnswers() != null && !request.getSecurityAnswers().isEmpty()) {
            verificationService.registerSecurityAnswers(savedUser, request.getSecurityAnswers());
        }

        // 7. Register initial password in RedisBloom & Password History
        bloomPasswordService.recordPassword(savedUser.getId(), request.getPassword());
        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(savedUser)
                .passwordHash(savedUser.getPassword())
                .build());

        // 8. Anchor registration event in Blockchain audit log
        blockchainAuditService.anchorAuditBatch(List.of(
                "USER_REGISTERED:id=" + savedUser.getId() + ":email=" + savedUser.getEmail() + ":time=" + OffsetDateTime.now()
        ));

        // 9. Build Auth Tokens
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", savedUser.getId().toString());
        extraClaims.put("must_change_password", false);

        String jwtToken = jwtService.generateToken(extraClaims, savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        Set<String> roleNames = savedUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .roles(roleNames)
                .mustChangePassword(false)
                .mnemonicRecoveryPhrase(mnemonicPhrase)
                .backupRecoveryCodes(backupCodes)
                .dynamicAttributes(savedUser.getDynamicAttributes())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String identifier = request.getEffectiveIdentifier();

        // 1. IP Burst Rate Limiting
        if (ipAddress != null && rateLimiterService.isIpRateLimited(ipAddress)) {
            throw new AuthException("Rate limit exceeded from this IP address. Please wait a moment.");
        }

        // 2. CAPTCHA Validation
        boolean isCaptchaValid = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaAnswer());
        if (!isCaptchaValid) {
            throw new AuthException("Invalid or expired CAPTCHA. Please solve the new challenge.");
        }

        // 3. Check 72-Hour Account Lockout
        if (rateLimiterService.isAccountLocked(identifier)) {
            long remainingSeconds = rateLimiterService.getRemainingLockoutSeconds(identifier);
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            throw new AuthException(String.format(
                    "Security Alert: This account exceeded %d failed attempts and is locked for 72 hours. " +
                    "Time remaining: %d hours %d minutes. Use the 'Unlock Account' security verification option.",
                    maxFailedAttempts, hours, minutes
            ));
        }

        // 4. Locate user by email or username
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElse(null);

        if (user == null) {
            rateLimiterService.recordFailedAttempt(identifier);
            throw new AuthException("Invalid username or password.");
        }

        // 5. Database-level Lockout verification
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new AuthException("Account is locked until " + user.getLockedUntil() + ". Complete security verification.");
        }

        // 6. Verify Password Credential
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) {
            int attempts = rateLimiterService.recordFailedAttempt(identifier);
            user.setFailedLoginAttempts(attempts);

            if (attempts > maxFailedAttempts) {
                user.setLockedUntil(OffsetDateTime.now().plusHours(lockoutDurationHours));
                user.setLockoutCount(user.getLockoutCount() + 1);
                userRepository.save(user);

                // Blockchain Audit Log for critical security lockout
                blockchainAuditService.anchorAuditBatch(List.of(
                        "ACCOUNT_LOCKED_72H:id=" + user.getId() + ":ip=" + ipAddress + ":attempts=" + attempts
                ));

                throw new AuthException(String.format(
                        "Maximum login attempts exceeded (%d). Account has been BLOCKED for 72 hours for your protection.",
                        maxFailedAttempts
                ));
            }

            userRepository.save(user);
            int remaining = Math.max(0, maxFailedAttempts - attempts + 1);
            throw new AuthException(String.format("Invalid credentials. %d attempt(s) remaining before a 72-hour lockout.", remaining));
        }

        // 7. Successful Authentication: Reset attempt counters
        rateLimiterService.resetFailedAttempts(identifier);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(OffsetDateTime.now());

        // Google-style background check: Inspect IP & device baseline
        if (verificationService.isAnomalousLogin(user, ipAddress, userAgent)) {
            log.warn("Security Alert: User {} logged in from anomalous device/IP: {} / {}", user.getEmail(), ipAddress, userAgent);
        }

        user.setLastKnownIp(ipAddress);
        user.setLastKnownDevice(userAgent);
        userRepository.save(user);

        // 8. Generate JWT Token (Including must_change_password flag)
        boolean mustChangePassword = Boolean.TRUE.equals(user.getMustChangePassword());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("must_change_password", mustChangePassword);

        String jwtToken = jwtService.generateToken(extraClaims, user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roleNames)
                .mustChangePassword(mustChangePassword)
                .dynamicAttributes(user.getDynamicAttributes())
                .build();
    }

    @Override
    @Transactional
    public TemporaryPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        // 1. Validate CAPTCHA
        boolean validCaptcha = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaAnswer());
        if (!validCaptcha) {
            throw new AuthException("Invalid or expired CAPTCHA.");
        }

        // 2. Locate User
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new AuthException("No account found matching: " + identifier));

        // 3. Verify Identity via chosen recovery mechanism
        boolean verified = false;
        String method = request.getRecoveryMethod().toUpperCase();

        if ("RECOVERY_PHRASE".equals(method)) {
            verified = recoveryKeyService.verifyRecoveryPhrase(user, request.getRecoveryValue());
        } else if ("BACKUP_CODE".equals(method)) {
            verified = recoveryKeyService.verifyAndConsumeBackupCode(user, request.getRecoveryValue());
        } else if ("SECURITY_QUESTION".equals(method)) {
            if (request.getQuestionId() == null || request.getSecurityAnswer() == null) {
                throw new AuthException("Question ID and Answer are required for security question verification.");
            }
            verified = verificationService.verifySecurityAnswer(user.getId(), request.getQuestionId(), request.getSecurityAnswer());
        } else {
            throw new AuthException("Invalid recovery method specified: " + method);
        }

        if (!verified) {
            throw new AuthException("Recovery verification failed. Incorrect credentials provided.");
        }

        // 4. Issue 24-hour Temporary Password & Require Password Change
        String tempPassword = tempPasswordService.issueTemporaryPassword(user);

        // Invalidate old refresh tokens
        refreshTokenRepository.deleteByUserId(user.getId());

        // Audit Anchor
        blockchainAuditService.anchorAuditBatch(List.of(
                "FORGOT_PASSWORD_RESET:id=" + user.getId() + ":method=" + method + ":time=" + OffsetDateTime.now()
        ));

        return TemporaryPasswordResponse.builder()
                .temporaryPassword(tempPassword)
                .expiresAt(user.getTempPasswordExpiry())
                .message("Temporary password generated. Log in and create your permanent password immediately.")
                .build();
    }

    @Override
    @Transactional
    public void changePassword(String currentUserEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AuthException("User not found: " + currentUserEmail));

        // 1. Verify current or temporary password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthException("Current password does not match.");
        }

        // 2. Validate confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("New password and confirmation password do not match.");
        }

        // 3. RedisBloom Check: Non-repetition of previous passwords
        if (bloomPasswordService.isPasswordRepeated(user.getId(), request.getNewPassword())) {
            throw new AuthException("Security Policy: You cannot reuse a previous password. Please choose a new password.");
        }

        // 4. RedisBloom Check: Breached passwords dictionary
        if (bloomPasswordService.isCommonBreachedPassword(request.getNewPassword())) {
            throw new AuthException("Security Policy: This password appears in common breach lists. Choose a more complex password.");
        }

        // 5. Update Password
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedNewPassword);
        tempPasswordService.clearTemporaryPasswordState(user);
        userRepository.save(user);

        // 6. Record in RedisBloom & History table
        bloomPasswordService.recordPassword(user.getId(), request.getNewPassword());
        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(user)
                .passwordHash(encodedNewPassword)
                .build());

        // 7. Revoke existing tokens: force user back to main login screen
        refreshTokenRepository.deleteByUserId(user.getId());

        // 8. Blockchain Audit Log
        blockchainAuditService.anchorAuditBatch(List.of(
                "PASSWORD_CHANGED:id=" + user.getId() + ":time=" + OffsetDateTime.now()
        ));
    }

    @Override
    @Transactional
    public void changeUsername(String currentUserEmail, ChangeUsernameRequest request) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AuthException("User not found: " + currentUserEmail));

        // Verify password before altering identity
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthException("Current password verification failed.");
        }

        String newUsername = request.getNewUsername().trim();
        if (newUsername.equalsIgnoreCase(user.getUsername())) {
            return;
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new AuthException("Username '" + newUsername + "' is already taken.");
        }

        user.setUsername(newUsername);
        userRepository.save(user);

        // Invalidate tokens: force user back to main login screen
        refreshTokenRepository.deleteByUserId(user.getId());

        blockchainAuditService.anchorAuditBatch(List.of(
                "USERNAME_CHANGED:id=" + user.getId() + ":new=" + newUsername + ":time=" + OffsetDateTime.now()
        ));
    }

    @Override
    public PasswordCheckResponse checkPassword(String currentUserEmail, PasswordCheckRequest request) {
        String pwd = request.getPassword();
        List<String> feedback = new ArrayList<>();
        int score = 0;

        if (pwd.length() >= 8) score += 20;
        if (pwd.length() >= 12) score += 20;
        if (Pattern.compile("[A-Z]").matcher(pwd).find()) score += 15;
        if (Pattern.compile("[a-z]").matcher(pwd).find()) score += 15;
        if (Pattern.compile("[0-9]").matcher(pwd).find()) score += 15;
        if (Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(pwd).find()) score += 15;

        // Check against RedisBloom
        boolean breached = bloomPasswordService.isCommonBreachedPassword(pwd);
        boolean repeated = false;

        if (currentUserEmail != null && !currentUserEmail.isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(currentUserEmail);
            if (userOpt.isPresent()) {
                repeated = bloomPasswordService.isPasswordRepeated(userOpt.get().getId(), pwd);
            }
        }

        if (breached) {
            score = Math.min(score, 25);
            feedback.add("This password matches a known compromised password from public data breaches.");
        }
        if (repeated) {
            feedback.add("This password was previously used by this account.");
        }
        if (pwd.length() < 12) {
            feedback.add("Consider increasing password length to 12+ characters for quantum and brute-force resistance.");
        }

        String rating;
        if (score >= 80 && !breached && !repeated) rating = "EXCELLENT";
        else if (score >= 60 && !breached) rating = "STRONG";
        else if (score >= 40) rating = "MODERATE";
        else rating = "WEAK";

        return PasswordCheckResponse.builder()
                .score(score)
                .rating(rating)
                .foundInBreachedList(breached)
                .repeatedInHistory(repeated)
                .feedback(feedback)
                .build();
    }

    @Override
    @Transactional
    public boolean unlockAccount(UnlockAccountRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new AuthException("Account not found: " + identifier));

        boolean verified = false;
        String type = request.getChallengeType() != null ? request.getChallengeType().toUpperCase() : "EMAIL_OTP";

        if ("SECURITY_QUESTION".equals(type)) {
            if (request.getQuestionId() == null || request.getSecurityAnswer() == null) {
                throw new AuthException("Question and answer are required.");
            }
            verified = verificationService.verifySecurityAnswer(user.getId(), request.getQuestionId(), request.getSecurityAnswer());
        } else {
            verified = verificationService.verifyOtpChallenge(user, type, request.getOtpCode());
        }

        if (verified) {
            rateLimiterService.unlockAccount(user.getEmail());
            if (user.getUsername() != null) rateLimiterService.unlockAccount(user.getUsername());
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            blockchainAuditService.anchorAuditBatch(List.of(
                    "ACCOUNT_UNLOCKED_EARLY:id=" + user.getId() + ":time=" + OffsetDateTime.now()
            ));
            return true;
        }

        return false;
    }

    @Override
    public String triggerAccountUnlockOtp(String identifier, String challengeType, String ip, String device) {
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new AuthException("Account not found: " + identifier));

        return verificationService.generateOtpChallenge(user, challengeType, "72H_LOCKOUT_UNLOCK", ip, device);
    }
}
