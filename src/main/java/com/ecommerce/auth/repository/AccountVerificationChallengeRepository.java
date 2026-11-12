package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.AccountVerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountVerificationChallengeRepository extends JpaRepository<AccountVerificationChallenge, UUID> {
    Optional<AccountVerificationChallenge> findTopByUserIdAndChallengeTypeAndIsVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, String challengeType, OffsetDateTime now);
}
