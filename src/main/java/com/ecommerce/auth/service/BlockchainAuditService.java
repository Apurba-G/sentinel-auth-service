package com.ecommerce.auth.service;

import java.util.List;

public interface BlockchainAuditService {

    /**
     * Compute Merkle Root of critical security events and anchor proof to blockchain ledger.
     * @param events List of serialized audit event strings (e.g. login failures, password changes)
     * @return Batch ID and Merkle root hash
     */
    String anchorAuditBatch(List<String> events);

    /**
     * Verifies if an event hash exists within an anchored Merkle root.
     */
    boolean verifyProof(String batchId, String eventHash, List<String> merkleProof);
}
