package com.usedcar.platform.approval;

import com.usedcar.platform.approval.dto.ApprovalRequest;
import com.usedcar.platform.approval.dto.ApprovalRow;
import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.common.BizException;
import com.usedcar.platform.report.InspectionReport;
import com.usedcar.platform.report.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ReportService reportService;

    public ApprovalController(ApprovalService approvalService, ReportService reportService) {
        this.approvalService = approvalService;
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse<List<ApprovalRow>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(approvalService.list(status));
    }

    @PostMapping("/{id}/approve")
    @RequireRole({"OPERATOR"})
    public ApiResponse<InspectionReport> approve(@PathVariable Long id,
                                                 @RequestBody(required = false) ApprovalRequest request) {
        String opinion = request == null ? null : request.opinion();
        return ApiResponse.ok(reportService.approve(id, opinion));
    }

    @PostMapping("/{id}/reject")
    @RequireRole({"OPERATOR"})
    public ApiResponse<?> reject(@PathVariable Long id,
                                 @RequestBody(required = false) ApprovalRequest request) {
        String opinion = request == null ? null : request.opinion();
        if (opinion == null || opinion.isBlank()) {
            throw new BizException("驳回必须填写审批意见");
        }
        return ApiResponse.ok(reportService.reject(id, opinion));
    }
}
