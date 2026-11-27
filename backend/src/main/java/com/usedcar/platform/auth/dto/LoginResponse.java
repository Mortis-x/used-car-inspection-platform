package com.usedcar.platform.auth.dto;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String realName,
        String role,
        String roleLabel
) {
}
