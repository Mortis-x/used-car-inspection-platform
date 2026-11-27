package com.usedcar.platform.vehicle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VehicleSaveRequest(
        @NotBlank(message = "VIN 不能为空")
        @Size(max = 17, message = "VIN 最长 17 位")
        String vin,
        @Size(max = 20, message = "车牌号过长") String plateNo,
        @NotBlank(message = "品牌不能为空") String brand,
        @NotBlank(message = "车型不能为空") String model,
        String color,
        LocalDate firstRegDate,
        @NotNull(message = "表显里程不能为空")
        @Min(value = 0, message = "里程不能为负数") Integer mileage,
        @Min(value = 0, message = "过户次数不能为负数") Integer transferCount
) {
}
