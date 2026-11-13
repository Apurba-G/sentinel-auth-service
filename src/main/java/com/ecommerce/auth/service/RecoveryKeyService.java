package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.User;

import java.util.List;

public interface RecoveryKeyService {

    /**
     * Generates a secure 12-word BIP-39 mnemonic recovery paragraph.
     */
    String generateMnemonicPhrase();

    /**
     * Generates 5 single-use alphanumeric backup codes (format: XXXX-XXXX).
     */
    List<String> generateBackupCodes(int count);

    /**
     * Hashes and saves the user's recovery phrase.
     */
    void saveRecoveryPhrase(User user, String plaintextPhrase);

    /**
     * Hashes and saves generated backup recovery codes.
     */
    void saveBackupCodes(User user, List<String> plaintextCodes);

    /**
     * Validates whether provided recovery paragraph matches the stored one-way hash.
     */
    boolean verifyRecoveryPhrase(User user, String providedPhrase);

    /**
     * Verifies and immediately consumes a single-use backup recovery code.
     */
    boolean verifyAndConsumeBackupCode(User user, String providedCode);
}
