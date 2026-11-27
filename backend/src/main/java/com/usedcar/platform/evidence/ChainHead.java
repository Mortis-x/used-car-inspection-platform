package com.usedcar.platform.evidence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chain_head")
public class ChainHead {

    @TableId(type = IdType.INPUT)
    private Integer id;

    private Long seqNo;

    private String lastHash;

    private LocalDateTime updatedAt;
}
