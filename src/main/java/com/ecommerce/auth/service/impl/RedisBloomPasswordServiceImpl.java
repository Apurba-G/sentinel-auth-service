package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.service.RedisBloomPasswordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisBloomPasswordServiceImpl implements RedisBloomPasswordService {

    private final RedissonClient redissonClient;

    private static final String BLOOM_USER_PWD_PREFIX = "sentinel:bloom:pwd:";
    private static final String BLOOM_BREACHED_PWD = "sentinel:bloom:breached_passwords";

    // Expected password changes per user: 100, False Positive Probability: 0.1% (0.001)
    private static final long EXPECTED_USER_INSERTIONS = 100L;
    private static final double USER_FALSE_POSITIVE_RATE = 0.001;

    @PostConstruct
    public void initBreachedPasswordsFilter() {
        try {
            RBloomFilter<String> breachedFilter = redissonClient.getBloomFilter(BLOOM_BREACHED_PWD);
            // Expected 500,000 breached dictionary words, False Positive: 0.01%
            breachedFilter.tryInit(500_000L, 0.0001);

            // Seed common weak passwords to ensure immediate protection
            List<String> commonBreached = List.of(
                    "password123", "12345678", "admin123", "welcome123",
                    "qwerty123", "password@123", "admin@2024", "letmein123"
            );
            for (String pwd : commonBreached) {
                breachedFilter.add(hashString(pwd.toLowerCase()));
            }
            log.info("SentinelAuth RedisBloom: Breached passwords filter initialized successfully.");
        } catch (Exception e) {
            log.warn("RedisBloom breached filter initialization deferred: {}", e.getMessage());
        }
    }

    @Override
    public boolean isPasswordRepeated(UUID userId, String rawPassword) {
        if (userId == null || rawPassword == null) {
            return false;
        }
        try {
            RBloomFilter<String> userFilter = redissonClient.getBloomFilter(BLOOM_USER_PWD_PREFIX + userId);
            userFilter.tryInit(EXPECTED_USER_INSERTIONS, USER_FALSE_POSITIVE_RATE);
            String candidateHash = hashUserPassword(userId, rawPassword);
            boolean exists = userFilter.contains(candidateHash);
            if (exists) {
                log.warn("RedisBloom DETECTED PASSWORD REPETITION: User {} tried reusing a previous password.", userId);
            }
            return exists;
        } catch (Exception e) {
            log.error("Error checking RedisBloom for user password history: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void recordPassword(UUID userId, String rawPassword) {
        if (userId == null || rawPassword == null) {
            return;
        }
        try {
            RBloomFilter<String> userFilter = redissonClient.getBloomFilter(BLOOM_USER_PWD_PREFIX + userId);
            userFilter.tryInit(EXPECTED_USER_INSERTIONS, USER_FALSE_POSITIVE_RATE);
            String candidateHash = hashUserPassword(userId, rawPassword);
            userFilter.add(candidateHash);
            log.info("SentinelAuth RedisBloom: Registered new password hash in user {} bloom filter.", userId);
        } catch (Exception e) {
            log.error("Error recording password in RedisBloom: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCommonBreachedPassword(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        try {
            RBloomFilter<String> breachedFilter = redissonClient.getBloomFilter(BLOOM_BREACHED_PWD);
            breachedFilter.tryInit(500_000L, 0.0001);
            return breachedFilter.contains(hashString(rawPassword.toLowerCase()));
        } catch (Exception e) {
            log.error("Error checking RedisBloom breached password: {}", e.getMessage());
            return false;
        }
    }

    private String hashUserPassword(UUID userId, String rawPassword) {
        return hashString(userId.toString() + ":" + rawPassword);
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing in JVM", e);
        }
    }
}
