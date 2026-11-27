package com.usedcar.platform.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.evidence.VerifyRecord;
import com.usedcar.platform.evidence.VerifyRecordMapper;
import com.usedcar.platform.report.InspectionReport;
import com.usedcar.platform.report.InspectionReportMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;
    private final VerifyRecordMapper verifyRecordMapper;
    private final InspectionReportMapper reportMapper;

    public AuditLogController(AuditLogMapper auditLogMapper,
                              VerifyRecordMapper verifyRecordMapper,
                              InspectionReportMapper reportMapper) {
        this.auditLogMapper = auditLogMapper;
        this.verifyRecordMapper = verifyRecordMapper;
        this.reportMapper = reportMapper;
    }

    /**
     * 检测单流转记录：合并单据自身与关联报告的审计日志，按时间正序展示。
     */
    @GetMapping("/inspections/{id}/timeline")
    public ApiResponse<List<AuditLog>> timeline(@PathVariable Long id) {
        List<Long> reportIds = reportMapper.selectList(Wrappers.<InspectionReport>lambdaQuery()
                        .eq(InspectionReport::getOrderId, id))
                .stream().map(InspectionReport::getId).toList();

        LambdaQueryWrapper<AuditLog> wrapper = Wrappers.<AuditLog>lambdaQuery()
                .and(w -> {
                    w.eq(AuditLog::getBizType, "ORDER").eq(AuditLog::getBizId, id);
                    if (!reportIds.isEmpty()) {
                        w.or(r -> r.eq(AuditLog::getBizType, "REPORT").in(AuditLog::getBizId, reportIds));
                    }
                })
                .orderByAsc(AuditLog::getId);
        return ApiResponse.ok(auditLogMapper.selectList(wrapper));
    }

    @GetMapping("/audit-logs")
    @RequireRole({"OPERATOR", "ADMIN"})
    public ApiResponse<Map<String, Object>> auditLogs(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @RequestParam(required = false) String action) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page - 1, 0) * safeSize;
        Long total = auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(action != null && !action.isBlank(), AuditLog::getAction, action));
        List<AuditLog> records = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(action != null && !action.isBlank(), AuditLog::getAction, action)
                .orderByDesc(AuditLog::getId)
                .last("limit " + safeSize + " offset " + offset));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("records", records);
        return ApiResponse.ok(result);
    }

    @GetMapping("/verify-records")
    @RequireRole({"OPERATOR", "ARBITRATOR", "ADMIN"})
    public ApiResponse<Map<String, Object>> verifyRecords(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) String reportNo,
                                                           @RequestParam(required = false) String result) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page - 1, 0) * safeSize;
        Long total = verifyRecordMapper.selectCount(Wrappers.<VerifyRecord>lambdaQuery()
                .like(reportNo != null && !reportNo.isBlank(), VerifyRecord::getReportNo, reportNo)
                .eq(result != null && !result.isBlank(), VerifyRecord::getResult, result));
        List<VerifyRecord> records = verifyRecordMapper.selectList(Wrappers.<VerifyRecord>lambdaQuery()
                .like(reportNo != null && !reportNo.isBlank(), VerifyRecord::getReportNo, reportNo)
                .eq(result != null && !result.isBlank(), VerifyRecord::getResult, result)
                .orderByDesc(VerifyRecord::getId)
                .last("limit " + safeSize + " offset " + offset));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResponse.ok(data);
    }
}
