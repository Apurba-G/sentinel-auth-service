package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.SecurityQuestion;
import com.ecommerce.auth.entity.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SecurityVerificationService {

    /**
     * Lists all active security questions available for selection during registration.
     */
    List<SecurityQuestion> getActiveSecurityQuestions();

    /**
     * Saves user's answers to chosen security questions (answers hashed).
     */
    void registerSecurityAnswers(User user, Map<UUID, String> questionAnswers);

    /**
     * Verifies user answer against the registered question hash.
     */
    boolean verifySecurityAnswer(UUID userId, UUID questionId, String rawAnswer);

    /**
     * Google-style background check: Inspects login context for anomalies (new IP / device).
     * @return true if sign-in is suspicious / anomalous
     */
    boolean isAnomalousLogin(User user, String ipAddress, String userAgent);

    /**
     * Generates a 6-digit security verification OTP challenge sent to email or phone.
     */
    String generateOtpChallenge(User user, String challengeType, String triggerReason, String ip, String device);

    /**
     * Verifies OTP challenge and unlocks account early if locked.
     */
    boolean verifyOtpChallenge(User user, String challengeType, String submittedCode);
}
