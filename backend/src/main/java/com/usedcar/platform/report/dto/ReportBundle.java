package com.usedcar.platform.report.dto;

import java.util.Map;

public record ReportBundle(
        Long reportId,
        String reportNo,
        Long taskId,
        Integer totalScore,
        String grade,
        Long priceLow,
        Long priceHigh,
        Map<String, Integer> categoryScores,
        String status
) {
}
