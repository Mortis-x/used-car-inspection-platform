package com.usedcar.platform.dispute.dto;

import jakarta.validation.constraints.NotBlank;

public record AddEvidenceRequest(
        @NotBlank(message = "材料类型不能为空") String type,
        Long refId,
        String remark
) {
}
