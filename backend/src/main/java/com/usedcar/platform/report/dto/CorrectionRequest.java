package com.usedcar.platform.report.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CorrectionRequest(
        @NotBlank(message = "更正原因不能为空") String reason,
        @NotEmpty(message = "至少提交一项更正内容") @Valid List<ItemChange> items
) {

    public record ItemChange(
            Long itemId,
            Integer score,
            Integer defectLevel,
            String remark
    ) {
    }
}
