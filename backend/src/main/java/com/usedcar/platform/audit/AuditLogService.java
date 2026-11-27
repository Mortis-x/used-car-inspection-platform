package com.usedcar.platform.audit;

import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.auth.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(String action, String bizType, Long bizId, String detail) {
        UserPrincipal principal = AuthContext.get();
        if (principal == null) {
            record(null, null, null, action, bizType, bizId, detail);
            return;
        }
        record(principal.id(), principal.username(), principal.role(), action, bizType, bizId, detail);
    }

    /**
     * 登录等无请求上下文场景使用：由调用方显式传入操作人身份。
     */
    public void record(Long userId, String username, String role,
                       String action, String bizType, Long bizId, String detail) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setRole(role);
        log.setAction(action);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setDetail(detail);
        log.setIp(currentIp());
        auditLogMapper.insert(log);
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
