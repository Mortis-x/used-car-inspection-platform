package com.usedcar.platform.evidence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verify_record")
public class VerifyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportNo;

    private LocalDateTime verifyTime;

    private String ip;

    private String userAgent;

    /** PASS / FAIL / NOT_FOUND */
    private String result;

    /** 不一致字段明细 JSON */
    private String diffFields;

    private String detail;
}
