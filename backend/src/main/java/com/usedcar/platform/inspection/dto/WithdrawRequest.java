package com.usedcar.platform.inspection.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank(message = "撤回原因不能为空") String reason
) {
}
