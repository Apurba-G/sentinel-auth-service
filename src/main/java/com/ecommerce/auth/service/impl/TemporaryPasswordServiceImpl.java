package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.RateLimiterService;
import com.ecommerce.auth.service.TemporaryPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemporaryPasswordServiceImpl implements TemporaryPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    @Value("${application.security.temporary-password.expiration-hours:24}")
    private int tempPasswordExpirationHours;

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%!&*";
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public String issueTemporaryPassword(User user) {
        String tempPassword = generateComplexTempPassword();
        OffsetDateTime now = OffsetDateTime.now();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setTempPasswordIssuedAt(now);
        user.setTempPasswordExpiry(now.plusHours(tempPasswordExpirationHours));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null); // Clear any 72-hour lockouts

        userRepository.save(user);

        // Also release Redis lockout
        rateLimiterService.unlockAccount(user.getEmail());
        if (user.getUsername() != null) {
            rateLimiterService.unlockAccount(user.getUsername());
        }

        log.info("Issued 24-hour temporary password for user {} with mandatory password update flag", user.getId());
        return tempPassword;
    }

    @Override
    public boolean isTemporaryPasswordActive(User user) {
        if (!Boolean.TRUE.equals(user.getMustChangePassword())) {
            return false;
        }
        if (user.getTempPasswordExpiry() == null) {
            return false;
        }
        return user.getTempPasswordExpiry().isAfter(OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void clearTemporaryPasswordState(User user) {
        user.setMustChangePassword(false);
        user.setTempPasswordIssuedAt(null);
        user.setTempPasswordExpiry(null);
        userRepository.save(user);
        log.info("Cleared temporary password state for user {}", user.getId());
    }

    private String generateComplexTempPassword() {
        // Enforces at least 1 uppercase, 1 lowercase, 1 digit, 1 special character, length 12
        StringBuilder sb = new StringBuilder(12);
        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        String allChars = UPPER + LOWER + DIGITS + SPECIAL;
        for (int i = 4; i < 12; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle characters
        char[] array = sb.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }
}
