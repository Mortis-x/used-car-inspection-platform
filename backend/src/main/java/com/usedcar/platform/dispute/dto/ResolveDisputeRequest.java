package com.usedcar.platform.dispute.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveDisputeRequest(
        @NotBlank(message = "裁决结果不能为空") String result,
        @NotBlank(message = "请填写裁决结论") String conclusion
) {
}
