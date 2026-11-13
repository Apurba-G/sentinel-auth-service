package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blockchain_audit_anchors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainAuditAnchor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", unique = true, nullable = false, length = 100)
    private String batchId;

    @Column(name = "merkle_root_hash", nullable = false, length = 128)
    private String merkleRootHash;

    @Column(name = "event_count", nullable = false)
    @Builder.Default
    private Integer eventCount = 0;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Column(name = "blockchain_network", nullable = false, length = 50)
    @Builder.Default
    private String blockchainNetwork = "POLYGON_AMOY";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMMITTED, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
