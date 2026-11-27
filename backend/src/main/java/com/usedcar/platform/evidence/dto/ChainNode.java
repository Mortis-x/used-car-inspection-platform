package com.usedcar.platform.evidence.dto;

import java.time.Instant;

/**
 * 链上的一条存证记录，用于独立校验链条连续性。
 */
public record ChainNode(
        long seqNo,
        String prevHash,
        String reportHash,
        String recordHash,
        Instant issuedAt
) {
}
