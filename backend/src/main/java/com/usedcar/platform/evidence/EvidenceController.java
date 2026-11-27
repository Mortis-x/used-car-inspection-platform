package com.usedcar.platform.evidence;

import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.evidence.dto.ChainVerification;
import com.usedcar.platform.evidence.dto.ReportVerification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/chain")
    public ApiResponse<List<EvidenceChainRecord>> chain() {
        return ApiResponse.ok(evidenceService.list());
    }

    @GetMapping("/head")
    @RequireRole({"OPERATOR", "ADMIN"})
    public ApiResponse<ChainHead> head() {
        return ApiResponse.ok(evidenceService.head());
    }

    @GetMapping("/verify")
    public ApiResponse<ChainVerification> verifyAll() {
        return ApiResponse.ok(evidenceService.verifyAll());
    }

    @GetMapping("/verify/{reportNo}")
    public ApiResponse<ReportVerification> verifyReport(@PathVariable String reportNo) {
        return ApiResponse.ok(evidenceService.verifyReport(reportNo));
    }
}
