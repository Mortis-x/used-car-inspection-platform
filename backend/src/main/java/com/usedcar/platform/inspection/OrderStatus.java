package com.usedcar.platform.inspection;

/**
 * 检测单状态。流转规则集中在 InspectionOrderService 中，避免散落各处。
 */
public enum OrderStatus {

    PENDING("待检测"),
    INSPECTING("检测中"),
    PENDING_REPORT("待出报告"),
    PENDING_APPROVAL("待复核"),
    ISSUED("已出报告"),
    WITHDRAWN("已撤回");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(String code) {
        for (OrderStatus status : values()) {
            if (status.name().equals(code)) {
                return status.label;
            }
        }
        return code;
    }
}
