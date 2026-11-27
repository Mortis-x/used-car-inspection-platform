package com.usedcar.platform.evidence.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开验真结果：不暴露车主等隐私信息，只返回车况与存证摘要。
 */
public record PublicVerifyResult(
        String reportNo,
        boolean valid,
        String message,
        String grade,
        Integer totalScore,
        Integer version,
        LocalDateTime issuedAt,
        Long chainSeq,
        String recordHash,
        String brand,
        String model,
        String plateMasked,
        Integer mileage,
        List<DefectEntry> defects,
        List<FieldDiff> diffs
) {
}
