package com.usedcar.platform.vehicle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("maintenance_record")
public class MaintenanceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long vehicleId;

    /** MAINTAIN 保养 / REPAIR 维修 / ACCIDENT 事故 */
    private String recordType;

    private LocalDate recordDate;

    private Integer mileage;

    private String provider;

    private String content;

    /** 金额，单位：分 */
    private Long amount;

    private LocalDateTime createdAt;
}
