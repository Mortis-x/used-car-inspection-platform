package com.usedcar.platform.auth;

/**
 * 题库要求的五类业务角色，外加一个仅用于运维的超管账号。
 */
public enum Role {
    INSPECTOR("检测师"),
    DEALER("车商"),
    BUYER("买家"),
    OPERATOR("平台运营员"),
    ARBITRATOR("纠纷仲裁员"),
    ADMIN("系统管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Role of(String code) {
        for (Role role : values()) {
            if (role.name().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知角色: " + code);
    }
}
