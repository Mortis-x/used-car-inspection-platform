package com.usedcar.platform.dispute;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispute_evidence")
public class DisputeEvidence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long disputeId;

    /** REPORT / CHAIN / VERIFY / ATTACHMENT / REMARK */
    private String type;

    private Long refId;

    private String title;

    private String content;

    private Long submittedBy;

    private LocalDateTime createdAt;
}
