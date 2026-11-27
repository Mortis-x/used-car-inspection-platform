package com.usedcar.platform.statistics;

import com.usedcar.platform.audit.AuditLogService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public StatisticsService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    public record CountItem(String name, long count) {
    }

    public record DailyItem(String date, long orders, long reports) {
    }

    public record Overview(
            String from,
            String to,
            long totalOrders,
            long issuedReports,
            long dealCount,
            long disputeCount,
            long pendingApprovals,
            long totalVehicles,
            long defectItems,
            double avgScore,
            long verifyTotal,
            long verifyPass,
            long verifyFail,
            List<CountItem> gradeDistribution,
            List<CountItem> statusDistribution,
            List<DailyItem> dailyTrend
    ) {
    }

    public Overview overview(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        long totalOrders = queryLong("SELECT COUNT(*) FROM inspection_order WHERE created_at >= ? AND created_at < ?", start, end);
        long issuedReports = queryLong(
                "SELECT COUNT(*) FROM inspection_report WHERE status IN ('ACTIVE','SUPERSEDED') AND issued_at >= ? AND issued_at < ?",
                start, end);
        long pendingApprovals = queryLong(
                "SELECT COUNT(*) FROM approval_task WHERE status = 'PENDING' AND apply_time >= ? AND apply_time < ?", start, end);
        long dealCount = queryLong(
                "SELECT COUNT(*) FROM intention_order WHERE status = 'ACCEPTED' AND updated_at >= ? AND updated_at < ?",
                start, end);
        long disputeCount = queryLong(
                "SELECT COUNT(*) FROM dispute_case WHERE created_at >= ? AND created_at < ?", start, end);
        long totalVehicles = queryLong("SELECT COUNT(*) FROM vehicle", null, null);
        long defectItems = queryLong("""
                SELECT COUNT(*) FROM inspection_item i
                JOIN inspection_order o ON o.id = i.order_id
                WHERE i.defect_level > 0 AND o.created_at >= ? AND o.created_at < ?
                """, start, end);
        Double avgScore = jdbcTemplate.queryForObject("""
                SELECT AVG(total_score) FROM inspection_report
                WHERE status IN ('ACTIVE','SUPERSEDED') AND issued_at >= ? AND issued_at < ?
                """, Double.class, start, end);
        long verifyTotal = queryLong("SELECT COUNT(*) FROM verify_record WHERE verify_time >= ? AND verify_time < ?", start, end);
        long verifyPass = queryLong(
                "SELECT COUNT(*) FROM verify_record WHERE result = 'PASS' AND verify_time >= ? AND verify_time < ?", start, end);
        long verifyFail = queryLong(
                "SELECT COUNT(*) FROM verify_record WHERE result = 'FAIL' AND verify_time >= ? AND verify_time < ?", start, end);

        List<CountItem> grades = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT grade, COUNT(*) AS c FROM inspection_report
                WHERE status IN ('ACTIVE','SUPERSEDED') AND issued_at >= ? AND issued_at < ?
                GROUP BY grade ORDER BY grade
                """, rs -> {
            grades.add(new CountItem(rs.getString("grade"), rs.getLong("c")));
        }, start, end);

        List<CountItem> statuses = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT status, COUNT(*) AS c FROM inspection_order
                WHERE created_at >= ? AND created_at < ? GROUP BY status ORDER BY c DESC
                """, rs -> {
            statuses.add(new CountItem(rs.getString("status"), rs.getLong("c")));
        }, start, end);

        Map<String, long[]> dailyMap = new LinkedHashMap<>();
        // 以区间结束日为基准，回填最近 14 天
        for (int i = 13; i >= 0; i--) {
            dailyMap.put(to.minusDays(i).toString(), new long[]{0, 0});
        }
        LocalDateTime trendStart = to.minusDays(13).atStartOfDay();
        jdbcTemplate.query("""
                SELECT DATE(created_at) AS d, COUNT(*) AS c FROM inspection_order
                WHERE created_at >= ? AND created_at < ? GROUP BY DATE(created_at)
                """, rs -> {
            String date = rs.getDate("d").toLocalDate().toString();
            dailyMap.computeIfAbsent(date, key -> new long[]{0, 0})[0] = rs.getLong("c");
        }, trendStart, end);
        jdbcTemplate.query("""
                SELECT DATE(issued_at) AS d, COUNT(*) AS c FROM inspection_report
                WHERE status IN ('ACTIVE','SUPERSEDED') AND issued_at >= ? AND issued_at < ?
                GROUP BY DATE(issued_at)
                """, rs -> {
            String date = rs.getDate("d").toLocalDate().toString();
            dailyMap.computeIfAbsent(date, key -> new long[]{0, 0})[1] = rs.getLong("c");
        }, trendStart, end);
        List<DailyItem> daily = new ArrayList<>();
        dailyMap.forEach((date, counts) -> daily.add(new DailyItem(date, counts[0], counts[1])));

        return new Overview(from.toString(), to.toString(), totalOrders, issuedReports,
                dealCount, disputeCount, pendingApprovals,
                totalVehicles, defectItems, avgScore == null ? 0 : Math.round(avgScore * 10) / 10.0,
                verifyTotal, verifyPass, verifyFail, grades, statuses, daily);
    }

    public byte[] export(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        Overview overview = overview(from, to);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet summary = workbook.createSheet("统计汇总");
            int rowIndex = 0;
            String[][] summaryRows = {
                    {"统计区间", from + " 至 " + to},
                    {"检测单总数", String.valueOf(overview.totalOrders())},
                    {"已出报告数", String.valueOf(overview.issuedReports())},
                    {"意向成交数", String.valueOf(overview.dealCount())},
                    {"纠纷案件数", String.valueOf(overview.disputeCount())},
                    {"待审批任务", String.valueOf(overview.pendingApprovals())},
                    {"车辆档案数", String.valueOf(overview.totalVehicles())},
                    {"缺陷项数", String.valueOf(overview.defectItems())},
                    {"平均得分", String.valueOf(overview.avgScore())},
                    {"验真次数", String.valueOf(overview.verifyTotal())},
                    {"验真通过", String.valueOf(overview.verifyPass())},
                    {"验真失败", String.valueOf(overview.verifyFail())}
            };
            for (String[] pair : summaryRows) {
                Row row = summary.createRow(rowIndex++);
                row.createCell(0).setCellValue(pair[0]);
                row.createCell(1).setCellValue(pair[1]);
            }
            rowIndex++;
            Row gradeHeader = summary.createRow(rowIndex++);
            gradeHeader.createCell(0).setCellValue("车况等级");
            gradeHeader.createCell(1).setCellValue("报告数量");
            for (CountItem item : overview.gradeDistribution()) {
                Row row = summary.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.name());
                row.createCell(1).setCellValue(item.count());
            }
            summary.setColumnWidth(0, 20 * 256);
            summary.setColumnWidth(1, 24 * 256);

            Sheet detail = workbook.createSheet("检测单明细");
            String[] headers = {"检测单号", "品牌", "车型", "车牌", "检测师", "状态", "创建时间", "报告编号", "总分", "等级", "报告版本", "出具时间"};
            Row headerRow = detail.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                detail.setColumnWidth(i, 18 * 256);
            }
            String sql = """
                    SELECT o.order_no, v.brand, v.model, v.plate_no, u.real_name AS inspector,
                           o.status, o.created_at, r.report_no, r.total_score, r.grade, r.version, r.issued_at
                    FROM inspection_order o
                    JOIN vehicle v ON v.id = o.vehicle_id
                    LEFT JOIN sys_user u ON u.id = o.inspector_id
                    LEFT JOIN inspection_report r ON r.order_id = o.id AND r.status IN ('ACTIVE','SUPERSEDED')
                    WHERE o.created_at >= ? AND o.created_at < ?
                    ORDER BY o.id DESC
                    """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, start, end);
            int index = 1;
            for (Map<String, Object> data : rows) {
                Row row = detail.createRow(index++);
                setCell(row, 0, data.get("order_no"));
                setCell(row, 1, data.get("brand"));
                setCell(row, 2, data.get("model"));
                setCell(row, 3, data.get("plate_no"));
                setCell(row, 4, data.get("inspector"));
                setCell(row, 5, data.get("status"));
                setCell(row, 6, data.get("created_at"));
                setCell(row, 7, data.get("report_no"));
                setCell(row, 8, data.get("total_score"));
                setCell(row, 9, data.get("grade"));
                setCell(row, 10, data.get("version"));
                setCell(row, 11, data.get("issued_at"));
            }

            workbook.write(output);
            auditLogService.record("EXPORT", "STATISTICS", null,
                    "导出统计报表 " + from + " 至 " + to + "，检测单 " + rows.size() + " 条");
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    private void setCell(Row row, int index, Object value) {
        Cell cell = row.createCell(index);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private long queryLong(String sql, LocalDateTime start, LocalDateTime end) {
        Long value = start == null
                ? jdbcTemplate.queryForObject(sql, Long.class)
                : jdbcTemplate.queryForObject(sql, Long.class, start, end);
        return value == null ? 0 : value;
    }
}
