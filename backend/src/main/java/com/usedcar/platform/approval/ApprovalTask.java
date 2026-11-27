package com.usedcar.platform.approval;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("approval_task")
public class ApprovalTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** REPORT / CORRECTION */
    private String bizType;

    private Long bizId;

    private Long orderId;

    /** PENDING / APPROVED / REJECTED / CANCELLED */
    private String status;

    private Long applyUserId;

    private LocalDateTime applyTime;

    private String applyReason;

    /** 审批载荷 JSON；报告更正场景存放拟修改的检测项列表 */
    private String payload;

    private Long approveUserId;

    private LocalDateTime approveTime;

    private String opinion;
}
