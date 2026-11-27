package com.usedcar.platform.template;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("inspection_template_item")
public class InspectionTemplateItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    /** EXTERIOR / INTERIOR / ENGINE / CHASSIS / ACCIDENT / WATER_FIRE */
    private String category;

    private String itemCode;

    private String itemName;

    /** 权重，用于总分汇总 */
    private Integer weight;

    private Integer maxScore;

    private String defectRule;

    private Integer sort;
}
