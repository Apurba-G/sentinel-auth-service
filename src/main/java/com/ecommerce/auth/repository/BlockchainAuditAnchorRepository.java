package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.BlockchainAuditAnchor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockchainAuditAnchorRepository extends JpaRepository<BlockchainAuditAnchor, UUID> {
    Optional<BlockchainAuditAnchor> findByBatchId(String batchId);
}
