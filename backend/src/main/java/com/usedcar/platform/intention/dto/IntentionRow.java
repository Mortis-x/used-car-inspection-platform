package com.usedcar.platform.intention.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntentionRow {

    private Long id;
    private Long reportId;
    private String reportNo;
    private Long orderId;
    private String brand;
    private String model;
    private String plateNo;
    private Integer mileage;
    private Long buyerId;
    private String buyerName;
    private String dealerName;
    private Long offerPrice;
    private Long dealerReplyPrice;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getStatusLabel() {
        return switch (status) {
            case "PENDING" -> "待车商响应";
            case "COUNTERED" -> "车商已还价";
            case "ACCEPTED" -> "已成交";
            case "REJECTED" -> "已拒绝";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
