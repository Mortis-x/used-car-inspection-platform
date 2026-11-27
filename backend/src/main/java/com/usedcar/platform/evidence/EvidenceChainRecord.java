package com.usedcar.platform.evidence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 存证链记录：只允许插入，任何业务代码都不得修改或删除。
 */
@Data
@TableName("evidence_chain")
public class EvidenceChainRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private String reportNo;

    private Long seqNo;

    private String prevHash;

    private String reportHash;

    private String recordHash;

    private LocalDateTime issuedAt;

    private LocalDateTime createdAt;
}
