package com.usedcar.platform.inspection.dto;

public record ItemUpdateRequest(
        Integer score,
        Integer defectLevel,
        String remark
) {
}
