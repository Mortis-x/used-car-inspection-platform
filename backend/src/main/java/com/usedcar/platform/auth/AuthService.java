package com.usedcar.platform.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.audit.AuditLogService;
import com.usedcar.platform.auth.dto.LoginRequest;
import com.usedcar.platform.auth.dto.LoginResponse;
import com.usedcar.platform.common.BizException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用，请联系平台运营员");
        }
        String token = jwtUtil.generate(user);
        auditLogService.record(user.getId(), user.getUsername(), user.getRole(),
                "LOGIN", "AUTH", user.getId(), "账号登录成功");
        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                Role.of(user.getRole()).getLabel());
    }
}
