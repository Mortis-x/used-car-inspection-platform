package com.usedcar.platform.evidence.dto;

/**
 * 缺陷条目：一条缺陷对应一个检测项，扣减以“分”为单位。
 */
public record DefectEntry(String itemCode, Integer defectLevel, Integer deduct) {
}
