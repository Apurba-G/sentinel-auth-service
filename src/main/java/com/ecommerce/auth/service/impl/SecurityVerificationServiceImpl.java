package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.entity.AccountVerificationChallenge;
import com.ecommerce.auth.entity.SecurityQuestion;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserSecurityAnswer;
import com.ecommerce.auth.repository.AccountVerificationChallengeRepository;
import com.ecommerce.auth.repository.SecurityQuestionRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.repository.UserSecurityAnswerRepository;
import com.ecommerce.auth.service.RateLimiterService;
import com.ecommerce.auth.service.SecurityVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityVerificationServiceImpl implements SecurityVerificationService {

    private final SecurityQuestionRepository questionRepository;
    private final UserSecurityAnswerRepository answerRepository;
    private final AccountVerificationChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    private final SecureRandom random = new SecureRandom();

    @Override
    public List<SecurityQuestion> getActiveSecurityQuestions() {
        return questionRepository.findAllByActiveTrue();
    }

    @Override
    @Transactional
    public void registerSecurityAnswers(User user, Map<UUID, String> questionAnswers) {
        if (questionAnswers == null || questionAnswers.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, String> entry : questionAnswers.entrySet()) {
            SecurityQuestion question = questionRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Security question not found: " + entry.getKey()));

            String normalized = entry.getValue().trim().toLowerCase();
            UserSecurityAnswer answer = UserSecurityAnswer.builder()
                    .user(user)
                    .question(question)
                    .answerHash(passwordEncoder.encode(normalized))
                    .build();
            answerRepository.save(answer);
        }
        log.info("Registered {} security answers for user {}", questionAnswers.size(), user.getId());
    }

    @Override
    public boolean verifySecurityAnswer(UUID userId, UUID questionId, String rawAnswer) {
        if (userId == null || questionId == null || rawAnswer == null) {
            return false;
        }
        Optional<UserSecurityAnswer> answerOpt = answerRepository.findByUserIdAndQuestionId(userId, questionId);
        if (answerOpt.isEmpty()) {
            return false;
        }
        String normalized = rawAnswer.trim().toLowerCase();
        return passwordEncoder.matches(normalized, answerOpt.get().getAnswerHash());
    }

    @Override
    public boolean isAnomalousLogin(User user, String ipAddress, String userAgent) {
        if (user.getLastKnownIp() == null || user.getLastKnownDevice() == null) {
            // First time login — record baseline
            return false;
        }
        boolean ipChanged = !user.getLastKnownIp().equalsIgnoreCase(ipAddress);
        boolean deviceChanged = !user.getLastKnownDevice().equalsIgnoreCase(userAgent);

        if (ipChanged && deviceChanged) {
            log.warn("ANOMALY DETECTED for user {}: Unknown IP ({}) and Unknown Device ({})",
                    user.getEmail(), ipAddress, userAgent);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public String generateOtpChallenge(User user, String challengeType, String triggerReason, String ip, String device) {
        // Generate high-entropy 6-digit numeric OTP
        int otp = 100000 + random.nextInt(900000);
        String code = String.valueOf(otp);

        AccountVerificationChallenge challenge = AccountVerificationChallenge.builder()
                .user(user)
                .challengeType(challengeType)
                .challengeCode(passwordEncoder.encode(code))
                .triggerReason(triggerReason)
                .ipAddress(ip)
                .deviceInfo(device)
                .isVerified(false)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        challengeRepository.save(challenge);
        log.info("SECURITY CHALLENGE [{}]: Generated 6-digit OTP [{}] for user {} (Trigger: {})",
                challengeType, code, user.getEmail(), triggerReason);
        return code;
    }

    @Override
    @Transactional
    public boolean verifyOtpChallenge(User user, String challengeType, String submittedCode) {
        if (user == null || submittedCode == null) {
            return false;
        }

        Optional<AccountVerificationChallenge> challengeOpt =
                challengeRepository.findTopByUserIdAndChallengeTypeAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        user.getId(), challengeType, OffsetDateTime.now()
                );

        if (challengeOpt.isEmpty()) {
            log.warn("Verification challenge not found or expired for user {}", user.getId());
            return false;
        }

        AccountVerificationChallenge challenge = challengeOpt.get();
        boolean matched = passwordEncoder.matches(submittedCode.trim(), challenge.getChallengeCode());

        if (matched) {
            challenge.setIsVerified(true);
            challengeRepository.save(challenge);

            // Successfully verified: unlock 72-hour lockout and reset attempts
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            rateLimiterService.unlockAccount(user.getEmail());

            log.info("Security challenge verified for user {}. Account is now fully UNLOCKED.", user.getEmail());
            return true;
        } else {
            log.warn("Security challenge verification FAILED for user {}.", user.getEmail());
            return false;
        }
    }
}
