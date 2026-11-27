package com.usedcar.platform.dispute.dto;

import com.usedcar.platform.dispute.DisputeCase;
import com.usedcar.platform.dispute.DisputeEvidence;

import java.time.LocalDateTime;
import java.util.List;

public record DisputeDetail(
        DisputeCase dispute,
        List<DisputeEvidence> evidence,
        ReportSummary report,
        ChainSummary chain,
        long verifyCount
) {

    public record ReportSummary(
            String reportNo,
            Integer version,
            Integer totalScore,
            String grade,
            String status,
            LocalDateTime issuedAt,
            String brand,
            String model,
            String plateNo
    ) {
    }

    public record ChainSummary(Long seqNo, String reportHash, String recordHash, String prevHash) {
    }
}
