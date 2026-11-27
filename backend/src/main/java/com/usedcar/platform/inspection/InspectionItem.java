package com.usedcar.platform.inspection;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("inspection_item")
public class InspectionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long templateItemId;

    private String category;

    private String itemCode;

    private String itemName;

    /** 模板项权重快照，用于评分汇总 */
    private Integer weight;

    /** 0-100，null 表示未填写 */
    private Integer score;

    /** 0无 1轻微 2一般 3严重，null 表示未填写 */
    private Integer defectLevel;

    private Integer defectDeduct;

    private String remark;

    private Integer sort;
}
