package com.usedcar.platform.inspection;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_order")
public class InspectionOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long vehicleId;

    private Long templateId;

    private Long inspectorId;

    private Long dealerId;

    /** 见 OrderStatus */
    private String status;

    private String remark;

    private String withdrawReason;

    private LocalDateTime submittedAt;

    private LocalDateTime issuedAt;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
