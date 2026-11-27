package com.usedcar.platform.dispute.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DisputeRow {

    private Long id;
    private String reportNo;
    private Long orderId;
    private Long applicantId;
    private String applicantRole;
    private String reason;
    private String description;
    private String status;
    private String result;
    private String conclusion;
    private Long arbitratorId;
    private String applicantName;
    private String arbitratorName;
    private String brand;
    private String model;
    private String plateNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getStatusLabel() {
        return switch (status) {
            case "OPEN" -> "待受理";
            case "ARBITRATING" -> "仲裁中";
            case "RESOLVED" -> "已裁决";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }
}
