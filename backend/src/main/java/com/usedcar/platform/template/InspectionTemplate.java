package com.usedcar.platform.template;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_template")
public class InspectionTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer version;

    private Integer status;

    private String remark;

    private Long createdBy;

    private LocalDateTime createdAt;
}
