package com.ecommerce.auth.service;

public interface RateLimiterService {

    /**
     * Check if client IP address exceeds DDoS / burst threshold (e.g., 60 requests/min).
     */
    boolean isIpRateLimited(String ipAddress);

    /**
     * Check if account is locked due to exceeding max attempts (> 4 attempts, locked for 72 hours).
     */
    boolean isAccountLocked(String identifier);

    /**
     * Get remaining lockout duration in seconds (up to 72 hours = 259,200s).
     */
    long getRemainingLockoutSeconds(String identifier);

    /**
     * Increment failed attempt count for the user.
     * If attempts exceed 4 (i.e. on 5th attempt), trigger 72-hour lockout.
     * @return current failed attempt count.
     */
    int recordFailedAttempt(String identifier);

    /**
     * Reset failed attempts after successful authentication.
     */
    void resetFailedAttempts(String identifier);

    /**
     * Explicitly unlock account following security challenge / Google-style verification.
     */
    void unlockAccount(String identifier);
}
