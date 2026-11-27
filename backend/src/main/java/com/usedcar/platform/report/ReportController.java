package com.usedcar.platform.report;

import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.report.dto.CorrectionRequest;
import com.usedcar.platform.report.dto.ReportBundle;
import com.usedcar.platform.report.dto.ReportDetail;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/orders/{orderId}/generate")
    @RequireRole({"INSPECTOR", "OPERATOR"})
    public ApiResponse<ReportBundle> generate(@PathVariable Long orderId,
                                              @RequestParam(required = false) String reason) {
        return ApiResponse.ok(reportService.generate(orderId, reason));
    }

    @GetMapping("/market")
    public ApiResponse<List<com.usedcar.platform.report.dto.ReportMarketRow>> market(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grade) {
        return ApiResponse.ok(reportService.market(keyword, grade));
    }

    @GetMapping("/{reportNo}")
    public ApiResponse<ReportDetail> detail(@PathVariable String reportNo) {
        return ApiResponse.ok(reportService.detail(reportNo));
    }

    @GetMapping
    public ApiResponse<List<InspectionReport>> list(@RequestParam Long orderId) {
        return ApiResponse.ok(reportService.listByOrder(orderId));
    }

    @PostMapping("/{reportNo}/corrections")
    @RequireRole({"INSPECTOR", "OPERATOR"})
    public ApiResponse<Long> submitCorrection(@PathVariable String reportNo,
                                              @Valid @RequestBody CorrectionRequest request) {
        return ApiResponse.ok(reportService.submitCorrection(reportNo, request));
    }
}
