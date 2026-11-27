package com.usedcar.platform.auth;

/**
 * 登录用户信息，放进 ThreadLocal 供 Service 层读取，避免层层传参。
 */
public record UserPrincipal(Long id, String username, String realName, String role) {

    public boolean hasRole(String... roles) {
        for (String r : roles) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
