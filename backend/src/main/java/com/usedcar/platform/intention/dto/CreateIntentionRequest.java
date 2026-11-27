package com.usedcar.platform.intention.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record CreateIntentionRequest(
        @NotBlank(message = "报告编号不能为空") String reportNo,
        @NotNull(message = "请填写出价") Long offerPrice,
        String message
) {
}
