package com.usedcar.platform.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批列表行：审批任务 + 检测单 + 车辆 + 报告摘要。
 */
@Data
public class ApprovalRow {

    private Long id;
    private String bizType;
    private Long bizId;
    private Long orderId;
    private String status;
    private Long applyUserId;
    private LocalDateTime applyTime;
    private String applyReason;
    private Long approveUserId;
    private LocalDateTime approveTime;
    private String opinion;

    private String orderNo;
    private String brand;
    private String model;
    private String plateNo;
    private String reportNo;
    private Integer totalScore;
    private String grade;
    private String applyName;

    public String getStatusLabel() {
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
