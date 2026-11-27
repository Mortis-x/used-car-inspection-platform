package com.usedcar.platform.inspection.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "请选择车辆") Long vehicleId,
        @NotNull(message = "请选择检测模板") Long templateId,
        @NotNull(message = "请指派检测师") Long inspectorId,
        String remark
) {
}
