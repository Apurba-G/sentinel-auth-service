package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.UserSecurityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSecurityAnswerRepository extends JpaRepository<UserSecurityAnswer, UUID> {
    List<UserSecurityAnswer> findByUserId(UUID userId);
    Optional<UserSecurityAnswer> findByUserIdAndQuestionId(UUID userId, UUID questionId);
}
