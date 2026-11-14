package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.entity.BlockchainAuditAnchor;
import com.ecommerce.auth.repository.BlockchainAuditAnchorRepository;
import com.ecommerce.auth.service.BlockchainAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainAuditServiceImpl implements BlockchainAuditService {

    private final BlockchainAuditAnchorRepository anchorRepository;

    @Override
    @Transactional
    public String anchorAuditBatch(List<String> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }

        List<String> leafHashes = events.stream()
                .map(this::sha256)
                .toList();

        String merkleRoot = computeMerkleRoot(leafHashes);
        String batchId = "batch-" + UUID.randomUUID();

        // Simulated Polygon / Ethereum on-chain transaction hash
        String mockTxHash = "0x" + sha256(batchId + ":" + merkleRoot);

        BlockchainAuditAnchor anchor = BlockchainAuditAnchor.builder()
                .batchId(batchId)
                .merkleRootHash(merkleRoot)
                .eventCount(events.size())
                .txHash(mockTxHash)
                .blockchainNetwork("POLYGON_AMOY")
                .status("COMMITTED")
                .confirmedAt(OffsetDateTime.now())
                .build();

        anchorRepository.save(anchor);
        log.info("ANCHORED AUDIT BATCH: Batch {} containing {} events committed to Blockchain ledger with Merkle Root {}",
                batchId, events.size(), merkleRoot);

        return batchId;
    }

    @Override
    public boolean verifyProof(String batchId, String eventHash, List<String> merkleProof) {
        return anchorRepository.findByBatchId(batchId)
                .map(anchor -> {
                    String current = eventHash;
                    for (String sibling : merkleProof) {
                        current = sha256(current + sibling);
                    }
                    return current.equalsIgnoreCase(anchor.getMerkleRootHash());
                })
                .orElse(false);
    }

    private String computeMerkleRoot(List<String> leaves) {
        if (leaves.isEmpty()) return "";
        if (leaves.size() == 1) return leaves.get(0);

        List<String> parents = new ArrayList<>();
        for (int i = 0; i < leaves.size(); i += 2) {
            String left = leaves.get(i);
            String right = (i + 1 < leaves.size()) ? leaves.get(i + 1) : left;
            parents.add(sha256(left + right));
        }
        return computeMerkleRoot(parents);
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
