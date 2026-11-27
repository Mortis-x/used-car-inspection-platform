package com.usedcar.platform.intention;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("intention_order")
public class IntentionOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private Long buyerId;

    /** 出价，单位：分 */
    private Long offerPrice;

    /** 车商还价，单位：分 */
    private Long dealerReplyPrice;

    /** PENDING / ACCEPTED / REJECTED / COUNTERED / CANCELLED */
    private String status;

    private String message;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
