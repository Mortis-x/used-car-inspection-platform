package com.usedcar.platform.attachment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attachment")
public class Attachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long itemId;

    private String fileName;

    private String originPath;

    private String thumbPath;

    /** 原图 SHA-256，用于附件层校验 */
    private String originHash;

    private String contentType;

    private Long fileSize;

    /** PENDING / PROCESSING / DONE / FAILED */
    private String status;

    private Integer retryCount;

    private String errorMsg;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
