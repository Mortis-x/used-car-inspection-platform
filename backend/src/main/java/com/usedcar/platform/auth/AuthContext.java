package com.usedcar.platform.auth;

import com.usedcar.platform.common.BizException;

public final class AuthContext {

    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(UserPrincipal principal) {
        HOLDER.set(principal);
    }

    public static UserPrincipal get() {
        return HOLDER.get();
    }

    public static UserPrincipal require() {
        UserPrincipal principal = HOLDER.get();
        if (principal == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return principal;
    }

    public static Long userId() {
        return require().id();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
