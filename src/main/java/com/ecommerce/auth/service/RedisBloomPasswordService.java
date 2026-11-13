package com.ecommerce.auth.service;

import java.util.UUID;

public interface RedisBloomPasswordService {

    /**
     * Checks whether a new candidate password matches any previously used password in the user's Bloom filter.
     * @param userId user identifier
     * @param rawPassword plaintext password candidate
     * @return true if password exists in historical Bloom filter (repetition detected)
     */
    boolean isPasswordRepeated(UUID userId, String rawPassword);

    /**
     * Records a newly accepted password into the user's RedisBloom filter.
     * @param userId user identifier
     * @param rawPassword plaintext password candidate
     */
    void recordPassword(UUID userId, String rawPassword);

    /**
     * Checks if password exists in the global known-breached-passwords Bloom filter.
     */
    boolean isCommonBreachedPassword(String rawPassword);
}
