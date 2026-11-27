package com.usedcar.platform.evidence.dto;

/** 报告内容与存证指纹的不一致字段 */
public record FieldDiff(String field, String stored, String current) {
}
