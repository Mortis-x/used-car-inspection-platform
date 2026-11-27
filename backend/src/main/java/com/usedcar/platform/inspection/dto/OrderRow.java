package com.usedcar.platform.inspection.dto;

import com.usedcar.platform.inspection.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测单列表行：检测单 + 车辆摘要 + 检测师姓名，供列表页一次查出。
 */
@Data
public class OrderRow {

    private Long id;
    private String orderNo;
    private Long vehicleId;
    private Long templateId;
    private Long inspectorId;
    private Long dealerId;
    private String status;
    private String remark;
    private String withdrawReason;
    private LocalDateTime submittedAt;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;

    private String brand;
    private String model;
    private String plateNo;
    private String vin;
    private String inspectorName;

    /** 已生效报告的编号，未出报告时为 null */
    private String reportNo;

    public String getStatusLabel() {
        return OrderStatus.labelOf(status);
    }
}
