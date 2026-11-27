package com.usedcar.platform.evidence.dto;

import java.time.LocalDateTime;

/**
 * 单份报告验真结果。
 */
public record ReportVerification(
        String reportNo,
        boolean valid,
        String message,
        Integer totalScore,
        String grade,
        LocalDateTime issuedAt,
        Long chainSeq,
        String recordHash
) {

    public static ReportVerification pass(String reportNo, Integer totalScore, String grade,
                                          LocalDateTime issuedAt, Long chainSeq, String recordHash) {
        return new ReportVerification(reportNo, true, "验真通过：报告内容与存证链一致",
                totalScore, grade, issuedAt, chainSeq, recordHash);
    }

    public static ReportVerification fail(String reportNo, String message) {
        return new ReportVerification(reportNo, false, message, null, null, null, null, null);
    }

    public static ReportVerification notFound(String reportNo) {
        return fail(reportNo, "未找到该报告的存证记录");
    }
}
