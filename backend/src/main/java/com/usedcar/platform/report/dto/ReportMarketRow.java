package com.usedcar.platform.report.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportMarketRow {

    private String reportNo;
    private Integer version;
    private Integer totalScore;
    private String grade;
    private Long priceLow;
    private Long priceHigh;
    private LocalDateTime issuedAt;
    private String brand;
    private String model;
    private String plateNo;
    private Integer mileage;
}
