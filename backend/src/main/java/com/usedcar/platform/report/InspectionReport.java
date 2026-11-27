package com.usedcar.platform.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_report")
public class InspectionReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportNo;

    private Long orderId;

    private Long vehicleId;

    private Integer version;

    private Integer totalScore;

    private String grade;

    /** 估价下限，单位：分 */
    private Long priceLow;

    /** 估价上限，单位：分 */
    private Long priceHigh;

    private String canonicalJson;

    private String pdfPath;

    /** DRAFT / ACTIVE / SUPERSEDED / VOID */
    private String status;

    private LocalDateTime issuedAt;

    private Long createdBy;

    private LocalDateTime createdAt;
}
