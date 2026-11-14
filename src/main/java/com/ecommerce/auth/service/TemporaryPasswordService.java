package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.User;

public interface TemporaryPasswordService {

    /**
     * Issues a secure temporary password after client authenticity is verified.
     * Sets user.mustChangePassword = true and expires in 24 hours.
     * @param user authenticated user
     * @return plaintext temporary password to display/send to client
     */
    String issueTemporaryPassword(User user);

    /**
     * Verifies if a user is currently using an active, unexpired temporary password.
     */
    boolean isTemporaryPasswordActive(User user);

    /**
     * Clears temporary password flag upon successful password change.
     */
    void clearTemporaryPasswordState(User user);
}
