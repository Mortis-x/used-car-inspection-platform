package com.usedcar.platform.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MaintenanceRequest(
        @NotBlank(message = "记录类型不能为空") String recordType,
        @NotNull(message = "记录日期不能为空") LocalDate recordDate,
        Integer mileage,
        String provider,
        String content,
        /** 金额，单位：分 */
        Long amount
) {
}
