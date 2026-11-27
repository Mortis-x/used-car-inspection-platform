package com.usedcar.platform.dispute;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispute_case")
public class DisputeCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private String reportNo;

    private Long orderId;

    private Long applicantId;

    private String applicantRole;

    private String reason;

    private String description;

    /** OPEN / ARBITRATING / RESOLVED / CLOSED */
    private String status;

    private Long arbitratorId;

    /** SUPPORTED / REJECTED */
    private String result;

    private String conclusion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
