package com.usedcar.platform.dispute.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDisputeRequest(
        @NotBlank(message = "报告编号不能为空") String reportNo,
        @NotBlank(message = "请填写争议事由") String reason,
        String description
) {
}
