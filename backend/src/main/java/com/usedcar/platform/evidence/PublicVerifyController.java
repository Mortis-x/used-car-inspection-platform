package com.usedcar.platform.evidence;

import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.evidence.dto.PublicVerifyResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/verify")
public class PublicVerifyController {

    private final PublicVerifyService publicVerifyService;

    public PublicVerifyController(PublicVerifyService publicVerifyService) {
        this.publicVerifyService = publicVerifyService;
    }

    @GetMapping("/{reportNo}")
    public ApiResponse<PublicVerifyResult> verify(@PathVariable String reportNo, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ApiResponse.ok(publicVerifyService.verify(reportNo, ip, request.getHeader("User-Agent")));
    }
}
