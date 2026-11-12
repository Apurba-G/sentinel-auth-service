package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.UserBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBackupCodeRepository extends JpaRepository<UserBackupCode, UUID> {
    List<UserBackupCode> findByUserIdAndUsedFalse(UUID userId);
}
