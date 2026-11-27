package com.usedcar.platform.dashboard;

import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.auth.Role;
import com.usedcar.platform.auth.UserPrincipal;
import com.usedcar.platform.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    public DashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record DashboardSummary(
            long pendingOrders,
            long inspecting,
            long pendingReport,
            long activeReports,
            long pendingApprovals,
            long openDisputes,
            long arbitratingDisputes,
            long myIntentions,
            long myVehicles,
            long todayVerifies,
            long myDisputes,
            long dealerIntentions
    ) {
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary() {
        UserPrincipal principal = AuthContext.require();
        Map<String, Long> orderStatus = new HashMap<>();
        jdbcTemplate.query("SELECT status, COUNT(*) AS c FROM inspection_order GROUP BY status",
                rs -> {
                    orderStatus.put(rs.getString("status"), rs.getLong("c"));
                });

        long activeReports = count("SELECT COUNT(*) FROM inspection_report WHERE status = 'ACTIVE'");
        long pendingApprovals = count("SELECT COUNT(*) FROM approval_task WHERE status = 'PENDING'");
        long openDisputes = count("SELECT COUNT(*) FROM dispute_case WHERE status = 'OPEN'");
        long arbitrating = count("SELECT COUNT(*) FROM dispute_case WHERE status = 'ARBITRATING'");
        long myIntentions = count("SELECT COUNT(*) FROM intention_order WHERE buyer_id = ? AND status IN ('PENDING','COUNTERED')", principal.id());
        long myVehicles = Role.DEALER.name().equals(principal.role())
                ? count("SELECT COUNT(*) FROM vehicle WHERE dealer_id = ?", principal.id())
                : count("SELECT COUNT(*) FROM vehicle");
        long todayVerifies = count("SELECT COUNT(*) FROM verify_record WHERE verify_time >= CURDATE()");
        long myDisputes = count(
                "SELECT COUNT(*) FROM dispute_case WHERE applicant_id = ? AND status IN ('OPEN','ARBITRATING')",
                principal.id());
        long dealerIntentions = count("""
                SELECT COUNT(*) FROM intention_order i
                JOIN inspection_report r ON r.id = i.report_id
                JOIN vehicle v ON v.id = r.vehicle_id
                WHERE i.status IN ('PENDING','COUNTERED') AND (v.dealer_id = ? OR v.dealer_id IS NULL)
                """, principal.id());

        return ApiResponse.ok(new DashboardSummary(
                orderStatus.getOrDefault("PENDING", 0L),
                orderStatus.getOrDefault("INSPECTING", 0L),
                orderStatus.getOrDefault("PENDING_REPORT", 0L),
                activeReports,
                pendingApprovals,
                openDisputes,
                arbitrating,
                myIntentions,
                myVehicles,
                todayVerifies,
                myDisputes,
                dealerIntentions));
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private long count(String sql) {
        return count(sql, new Object[0]);
    }
}
