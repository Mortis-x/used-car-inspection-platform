package com.usedcar.platform.evidence.dto;

import java.time.Instant;
import java.util.List;

/**
 * 参与存证的报告结构化快照。
 * 注意：这里只放纳入指纹的字段，展示用字段（如照片、备注）不进入快照。
 */
public record ReportSnapshot(
        String reportNo,
        String vin,
        Integer mileage,
        Integer totalScore,
        String grade,
        List<DefectEntry> defects,
        Long priceLow,
        Long priceHigh,
        Instant issuedAt
) {
}
