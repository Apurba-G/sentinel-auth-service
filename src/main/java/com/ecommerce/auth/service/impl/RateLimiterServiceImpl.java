package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterServiceImpl implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${application.security.lockout.max-failed-attempts:4}")
    private int maxFailedAttempts;

    @Value("${application.security.lockout.lockout-duration-hours:72}")
    private int lockoutDurationHours;

    private static final String PREFIX_IP_RATE = "sentinel:rate:ip:";
    private static final String PREFIX_ATTEMPTS = "sentinel:auth:attempts:";
    private static final String PREFIX_LOCKOUT = "sentinel:auth:locked:";

    private static final int IP_REQUEST_LIMIT_PER_MINUTE = 60;

    @Override
    public boolean isIpRateLimited(String ipAddress) {
        String key = PREFIX_IP_RATE + ipAddress;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        boolean rateLimited = count != null && count > IP_REQUEST_LIMIT_PER_MINUTE;
        if (rateLimited) {
            log.warn("IP {} exceeded rate limit with {} requests in 1 minute window", ipAddress, count);
        }
        return rateLimited;
    }

    @Override
    public boolean isAccountLocked(String identifier) {
        String lockKey = PREFIX_LOCKOUT + identifier.toLowerCase();
        Boolean hasKey = redisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public long getRemainingLockoutSeconds(String identifier) {
        String lockKey = PREFIX_LOCKOUT + identifier.toLowerCase();
        Long expire = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return (expire != null && expire > 0) ? expire : 0L;
    }

    @Override
    public int recordFailedAttempt(String identifier) {
        String key = PREFIX_ATTEMPTS + identifier.toLowerCase();
        Long count = redisTemplate.opsForValue().increment(key);

        // Retain failed attempts window for 24 hours
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofHours(24));
        }

        int currentAttempts = count != null ? count.intValue() : 1;
        log.warn("Account [{}] recorded failed attempt #{} of max {}", identifier, currentAttempts, maxFailedAttempts);

        // If attempts exceed 4 (i.e. on 5th attempt), enforce strict 72-hour lockout
        if (currentAttempts > maxFailedAttempts) {
            String lockKey = PREFIX_LOCKOUT + identifier.toLowerCase();
            long lockSeconds = Duration.ofHours(lockoutDurationHours).toSeconds();
            redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofSeconds(lockSeconds));
            log.error("CRITICAL: Account [{}] exceeded {} attempts! Account is LOCKED for {} hours ({} seconds)",
                    identifier, maxFailedAttempts, lockoutDurationHours, lockSeconds);
        }

        return currentAttempts;
    }

    @Override
    public void resetFailedAttempts(String identifier) {
        String key = PREFIX_ATTEMPTS + identifier.toLowerCase();
        redisTemplate.delete(key);
    }

    @Override
    public void unlockAccount(String identifier) {
        String lockKey = PREFIX_LOCKOUT + identifier.toLowerCase();
        String attemptsKey = PREFIX_ATTEMPTS + identifier.toLowerCase();
        redisTemplate.delete(lockKey);
        redisTemplate.delete(attemptsKey);
        log.info("Account [{}] was successfully unlocked after security verification.", identifier);
    }
}
