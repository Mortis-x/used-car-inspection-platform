package com.usedcar.platform.report;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usedcar.platform.approval.ApprovalTask;
import com.usedcar.platform.approval.ApprovalTaskMapper;
import com.usedcar.platform.audit.AuditLogService;
import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.auth.UserPrincipal;
import com.usedcar.platform.common.BizException;
import com.usedcar.platform.common.RedisLockService;
import com.usedcar.platform.evidence.Canonicalizer;
import com.usedcar.platform.evidence.EvidenceService;
import com.usedcar.platform.evidence.HashChain;
import com.usedcar.platform.evidence.dto.DefectEntry;
import com.usedcar.platform.evidence.dto.ReportSnapshot;
import com.usedcar.platform.inspection.InspectionItem;
import com.usedcar.platform.inspection.InspectionItemMapper;
import com.usedcar.platform.inspection.InspectionOrder;
import com.usedcar.platform.inspection.InspectionOrderMapper;
import com.usedcar.platform.inspection.OrderStatus;
import com.usedcar.platform.report.dto.CorrectionRequest;
import com.usedcar.platform.report.dto.ReportBundle;
import com.usedcar.platform.report.dto.ReportDetail;
import com.usedcar.platform.scoring.ScoringEngine;
import com.usedcar.platform.vehicle.Vehicle;
import com.usedcar.platform.vehicle.VehicleMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ReportService {

    private static final long BASE_PRICE_CENTS = 12_000_000L;
    private static final int[] LEVEL_DEDUCT = {0, 3, 6, 10};
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InspectionReportMapper reportMapper;
    private final InspectionOrderMapper orderMapper;
    private final InspectionItemMapper itemMapper;
    private final VehicleMapper vehicleMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final EvidenceService evidenceService;
    private final Canonicalizer canonicalizer;
    private final RedisLockService redisLockService;
    private final AuditLogService auditLogService;
    private final com.usedcar.platform.attachment.AttachmentService attachmentService;

    @Value("${app.rules.report-valid-days:90}")
    private int reportValidDays;

    public ReportService(InspectionReportMapper reportMapper,
                         InspectionOrderMapper orderMapper,
                         InspectionItemMapper itemMapper,
                         VehicleMapper vehicleMapper,
                         ApprovalTaskMapper approvalTaskMapper,
                         EvidenceService evidenceService,
                         Canonicalizer canonicalizer,
                         RedisLockService redisLockService,
                         AuditLogService auditLogService,
                         com.usedcar.platform.attachment.AttachmentService attachmentService) {
        this.reportMapper = reportMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.vehicleMapper = vehicleMapper;
        this.approvalTaskMapper = approvalTaskMapper;
        this.evidenceService = evidenceService;
        this.canonicalizer = canonicalizer;
        this.redisLockService = redisLockService;
        this.auditLogService = auditLogService;
        this.attachmentService = attachmentService;
    }

    /**
     * 生成报告草稿并提交复核：此时不写存证链，报告在审批通过后才生效。
     */
    @Transactional
    public ReportBundle generate(Long orderId, String reason) {
        InspectionOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "检测单不存在");
        }
        if (!OrderStatus.PENDING_REPORT.name().equals(order.getStatus())) {
            throw new BizException("只有待出报告状态可以生成报告");
        }
        Long exists = reportMapper.selectCount(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getOrderId, orderId));
        if (exists != null && exists > 0) {
            throw new BizException("该检测单已生成报告，请勿重复生成");
        }

        List<InspectionItem> items = itemMapper.selectList(Wrappers.<InspectionItem>lambdaQuery()
                .eq(InspectionItem::getOrderId, orderId)
                .orderByAsc(InspectionItem::getSort));
        ScoringEngine.Result result = ScoringEngine.score(items);
        long center = BASE_PRICE_CENTS * result.totalScore() / 100;
        long priceLow = center * 92 / 100 / 1000 * 1000;
        long priceHigh = center * 108 / 100 / 1000 * 1000;

        InspectionReport report = new InspectionReport();
        report.setReportNo(nextReportNo());
        report.setOrderId(orderId);
        report.setVehicleId(order.getVehicleId());
        report.setVersion(1);
        report.setTotalScore(result.totalScore());
        report.setGrade(result.grade());
        report.setPriceLow(priceLow);
        report.setPriceHigh(priceHigh);
        report.setStatus("DRAFT");
        report.setIssuedAt(LocalDateTime.now());
        report.setCreatedBy(AuthContext.userId());
        insertWithReportNoRetry(report);

        ApprovalTask task = new ApprovalTask();
        task.setBizType("REPORT");
        task.setBizId(report.getId());
        task.setOrderId(orderId);
        task.setStatus("PENDING");
        task.setApplyUserId(AuthContext.userId());
        task.setApplyTime(LocalDateTime.now());
        task.setApplyReason(reason);
        approvalTaskMapper.insert(task);

        order.setStatus(OrderStatus.PENDING_APPROVAL.name());
        orderMapper.updateById(order);

        auditLogService.record("APPLY", "REPORT", report.getId(),
                "生成报告 " + report.getReportNo() + "，总分 " + report.getTotalScore() + "，提交复核");

        return new ReportBundle(report.getId(), report.getReportNo(), task.getId(),
                report.getTotalScore(), report.getGrade(), priceLow, priceHigh,
                result.categoryScores(), report.getStatus());
    }

    /**
     * 复核通过：校验同车同周期唯一、规范化、写哈希链、报告生效、检测单置为已出报告。
     */
    @Transactional
    public InspectionReport approve(Long taskId, String opinion) {
        ApprovalTask task = approvalTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(404, "审批任务不存在");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BizException("该审批任务已处理");
        }
        if ("CORRECTION".equals(task.getBizType())) {
            return approveCorrection(task, opinion);
        }
        if (!"REPORT".equals(task.getBizType())) {
            throw new BizException("暂不支持该类型审批");
        }
        return approveReport(task, opinion);
    }

    private InspectionReport approveReport(ApprovalTask task, String opinion) {
        InspectionReport report = reportMapper.selectById(task.getBizId());
        if (report == null || !"DRAFT".equals(report.getStatus())) {
            throw new BizException("报告状态异常，无法通过审批");
        }
        InspectionOrder order = orderMapper.selectById(task.getOrderId());
        if (order == null) {
            throw new BizException(404, "检测单不存在");
        }

        String lockKey = "lock:report:issue:" + report.getVehicleId();
        String lockToken = redisLockService.tryLock(lockKey, Duration.ofSeconds(15));
        if (lockToken == null) {
            throw new BizException("该车辆正在出具报告，请稍后重试");
        }
        try {
            ensureNoActiveReport(report);
            // 必须先截断到秒再写库：MySQL DATETIME(0) 会四舍五入小数秒，
            // 若直接用当前时间，入库值与参与哈希的时间会相差 1 秒，导致链验真失败。
            Instant issuedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

            Vehicle vehicle = vehicleMapper.selectById(report.getVehicleId());
            if (vehicle == null) {
                throw new BizException(404, "车辆不存在");
            }
            List<DefectEntry> defects = defectsOf(task.getOrderId());
            ReportSnapshot snapshot = new ReportSnapshot(
                    report.getReportNo(),
                    vehicle.getVin(),
                    vehicle.getMileage(),
                    report.getTotalScore(),
                    report.getGrade(),
                    defects,
                    report.getPriceLow(),
                    report.getPriceHigh(),
                    issuedAt);

            String canonicalJson = canonicalizer.canonicalize(snapshot);
            String reportHash = HashChain.sha256Hex(canonicalJson);

            evidenceService.append(report, reportHash, issuedAt);

            report.setCanonicalJson(canonicalJson);
            report.setStatus("ACTIVE");
            report.setIssuedAt(LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()));
            reportMapper.updateById(report);

            task.setStatus("APPROVED");
            task.setApproveUserId(AuthContext.userId());
            task.setApproveTime(LocalDateTime.now());
            task.setOpinion(opinion);
            approvalTaskMapper.updateById(task);

            order.setStatus(OrderStatus.ISSUED.name());
            order.setIssuedAt(LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()));
            orderMapper.updateById(order);

            auditLogService.record("ISSUE", "REPORT", report.getId(),
                    "复核通过并存证，报告编号 " + report.getReportNo() + "，记录指纹 " + reportHash);
            return report;
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    @Transactional
    public ApprovalTask reject(Long taskId, String opinion) {
        ApprovalTask task = approvalTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(404, "审批任务不存在");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BizException("该审批任务已处理");
        }
        InspectionReport report = reportMapper.selectById(task.getBizId());
        if (report != null && "DRAFT".equals(report.getStatus())) {
            report.setStatus("VOID");
            reportMapper.updateById(report);
        }
        InspectionOrder order = orderMapper.selectById(task.getOrderId());
        if (order != null && OrderStatus.PENDING_APPROVAL.name().equals(order.getStatus())) {
            order.setStatus(OrderStatus.INSPECTING.name());
            orderMapper.updateById(order);
        }
        task.setStatus("REJECTED");
        task.setApproveUserId(AuthContext.userId());
        task.setApproveTime(LocalDateTime.now());
        task.setOpinion(opinion);
        approvalTaskMapper.updateById(task);

        auditLogService.record("REJECT", "REPORT", report == null ? null : report.getId(),
                "复核驳回：" + (opinion == null ? "未填写意见" : opinion));
        return task;
    }

    /**
     * 发起报告更正申请：只提交拟修改项，审批通过后才生效并生成新版本。
     */
    @Transactional
    public Long submitCorrection(String reportNo, CorrectionRequest request) {
        InspectionReport report = reportMapper.selectOne(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getReportNo, reportNo)
                .last("limit 1"));
        if (report == null || !"ACTIVE".equals(report.getStatus())) {
            throw new BizException("只有已生效的报告可以申请更正");
        }
        InspectionOrder order = orderMapper.selectById(report.getOrderId());
        if (order == null) {
            throw new BizException(404, "检测单不存在");
        }
        UserPrincipal principal = AuthContext.require();
        boolean allowed = principal.hasRole("OPERATOR", "ADMIN")
                || Objects.equals(order.getInspectorId(), principal.id());
        if (!allowed) {
            throw new BizException(403, "只有被指派的检测师或平台运营员可以申请更正");
        }
        Long pending = approvalTaskMapper.selectCount(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getOrderId, order.getId())
                .eq(ApprovalTask::getBizType, "CORRECTION")
                .eq(ApprovalTask::getStatus, "PENDING"));
        if (pending != null && pending > 0) {
            throw new BizException("该检测单已有待处理的更正申请");
        }

        for (CorrectionRequest.ItemChange change : request.items()) {
            InspectionItem item = itemMapper.selectById(change.itemId());
            if (item == null || !Objects.equals(item.getOrderId(), order.getId())) {
                throw new BizException(404, "更正项不属于该检测单");
            }
            if (change.score() != null && (change.score() < 0 || change.score() > 100)) {
                throw new BizException("评分必须在 0 到 100 之间");
            }
            if (change.defectLevel() != null && (change.defectLevel() < 0 || change.defectLevel() > 3)) {
                throw new BizException("缺陷等级不合法");
            }
        }

        String payload;
        try {
            payload = MAPPER.writeValueAsString(request.items());
        } catch (Exception e) {
            throw new BizException("更正内容序列化失败");
        }

        ApprovalTask task = new ApprovalTask();
        task.setBizType("CORRECTION");
        task.setBizId(report.getId());
        task.setOrderId(order.getId());
        task.setStatus("PENDING");
        task.setApplyUserId(principal.id());
        task.setApplyTime(LocalDateTime.now());
        task.setApplyReason(request.reason());
        task.setPayload(payload);
        approvalTaskMapper.insert(task);

        auditLogService.record("CORRECT_APPLY", "REPORT", report.getId(),
                "申请更正报告 " + reportNo + "，共 " + request.items().size() + " 项");
        return task.getId();
    }

    /**
     * 更正通过：应用修改项，生成 v(n+1) 报告并写入存证链，原报告归档保留。
     */
    private InspectionReport approveCorrection(ApprovalTask task, String opinion) {
        InspectionReport original = reportMapper.selectById(task.getBizId());
        if (original == null || !"ACTIVE".equals(original.getStatus())) {
            throw new BizException("原报告状态异常，无法更正");
        }
        InspectionOrder order = orderMapper.selectById(task.getOrderId());
        if (order == null) {
            throw new BizException(404, "检测单不存在");
        }
        List<CorrectionRequest.ItemChange> changes = readChanges(task.getPayload());
        if (changes.isEmpty()) {
            throw new BizException("更正内容为空");
        }

        String lockKey = "lock:report:issue:" + original.getVehicleId();
        String lockToken = redisLockService.tryLock(lockKey, Duration.ofSeconds(15));
        if (lockToken == null) {
            throw new BizException("该车辆正在出具报告，请稍后重试");
        }
        try {
            for (CorrectionRequest.ItemChange change : changes) {
                InspectionItem item = itemMapper.selectById(change.itemId());
                if (item == null || !Objects.equals(item.getOrderId(), order.getId())) {
                    throw new BizException(404, "更正项不属于该检测单");
                }
                item.setScore(change.score());
                item.setDefectLevel(change.defectLevel());
                item.setRemark(change.remark());
                item.setDefectDeduct(change.defectLevel() == null ? null : LEVEL_DEDUCT[change.defectLevel()]);
                itemMapper.updateById(item);
            }

            List<InspectionItem> items = itemMapper.selectList(Wrappers.<InspectionItem>lambdaQuery()
                    .eq(InspectionItem::getOrderId, order.getId())
                    .orderByAsc(InspectionItem::getSort));
            ScoringEngine.Result result = ScoringEngine.score(items);
            long center = BASE_PRICE_CENTS * result.totalScore() / 100;
            long priceLow = center * 92 / 100 / 1000 * 1000;
            long priceHigh = center * 108 / 100 / 1000 * 1000;

            InspectionReport latest = reportMapper.selectOne(Wrappers.<InspectionReport>lambdaQuery()
                    .eq(InspectionReport::getOrderId, order.getId())
                    .orderByDesc(InspectionReport::getVersion)
                    .last("limit 1"));
            int nextVersion = latest == null ? 1 : latest.getVersion() + 1;

            InspectionReport corrected = new InspectionReport();
            corrected.setReportNo(nextReportNo());
            corrected.setOrderId(order.getId());
            corrected.setVehicleId(original.getVehicleId());
            corrected.setVersion(nextVersion);
            corrected.setTotalScore(result.totalScore());
            corrected.setGrade(result.grade());
            corrected.setPriceLow(priceLow);
            corrected.setPriceHigh(priceHigh);
            corrected.setStatus("DRAFT");
            corrected.setIssuedAt(LocalDateTime.now());
            corrected.setCreatedBy(AuthContext.userId());
            insertWithReportNoRetry(corrected);

            Instant issuedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            Vehicle vehicle = vehicleMapper.selectById(corrected.getVehicleId());
            List<DefectEntry> defects = defectsOf(order.getId());
            ReportSnapshot snapshot = new ReportSnapshot(
                    corrected.getReportNo(), vehicle.getVin(), vehicle.getMileage(),
                    corrected.getTotalScore(), corrected.getGrade(), defects,
                    corrected.getPriceLow(), corrected.getPriceHigh(), issuedAt);
            String canonicalJson = canonicalizer.canonicalize(snapshot);
            String reportHash = HashChain.sha256Hex(canonicalJson);
            evidenceService.append(corrected, reportHash, issuedAt);

            corrected.setCanonicalJson(canonicalJson);
            corrected.setStatus("ACTIVE");
            corrected.setIssuedAt(LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()));
            reportMapper.updateById(corrected);

            original.setStatus("SUPERSEDED");
            reportMapper.updateById(original);

            task.setStatus("APPROVED");
            task.setApproveUserId(AuthContext.userId());
            task.setApproveTime(LocalDateTime.now());
            task.setOpinion(opinion);
            approvalTaskMapper.updateById(task);

            order.setIssuedAt(LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()));
            orderMapper.updateById(order);

            auditLogService.record("ISSUE", "REPORT", corrected.getId(),
                    "更正生效，生成 v" + nextVersion + " 报告 " + corrected.getReportNo()
                            + "，原报告 " + original.getReportNo() + " 已归档");
            return corrected;
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private List<CorrectionRequest.ItemChange> readChanges(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(payload, new TypeReference<List<CorrectionRequest.ItemChange>>() {
            });
        } catch (Exception e) {
            throw new BizException("更正内容解析失败");
        }
    }

    public ReportDetail detail(String reportNo) {
        InspectionReport report = reportMapper.selectOne(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getReportNo, reportNo)
                .last("limit 1"));
        if (report == null) {
            throw new BizException(404, "报告不存在");
        }
        Vehicle vehicle = vehicleMapper.selectById(report.getVehicleId());
        InspectionOrder order = orderMapper.selectById(report.getOrderId());
        List<InspectionItem> items = itemMapper.selectList(Wrappers.<InspectionItem>lambdaQuery()
                .eq(InspectionItem::getOrderId, report.getOrderId())
                .orderByAsc(InspectionItem::getSort));
        return new ReportDetail(report, vehicle, order, items, defectsOf(report.getOrderId()),
                attachmentService.list(report.getOrderId()));
    }

    public List<InspectionReport> listByOrder(Long orderId) {
        return reportMapper.selectList(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getOrderId, orderId)
                .orderByDesc(InspectionReport::getVersion));
    }

    /**
     * 报告中心：所有已生效报告，供买家/车商浏览与发起交易。
     */
    public List<com.usedcar.platform.report.dto.ReportMarketRow> market(String keyword, String grade) {
        return reportMapper.selectMarket(keyword, grade);
    }

    private void ensureNoActiveReport(InspectionReport report) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(reportValidDays);
        Long active = reportMapper.selectCount(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getVehicleId, report.getVehicleId())
                .eq(InspectionReport::getStatus, "ACTIVE")
                .ne(InspectionReport::getId, report.getId())
                .ge(InspectionReport::getIssuedAt, cutoff));
        if (active != null && active > 0) {
            throw new BizException("该车辆在 " + reportValidDays + " 天有效期内已有生效报告，同一周期只能出具一份");
        }
    }

    private List<DefectEntry> defectsOf(Long orderId) {
        List<InspectionItem> items = itemMapper.selectList(Wrappers.<InspectionItem>lambdaQuery()
                .eq(InspectionItem::getOrderId, orderId)
                .gt(InspectionItem::getDefectLevel, 0)
                .orderByAsc(InspectionItem::getSort));
        List<DefectEntry> defects = new ArrayList<>();
        for (InspectionItem item : items) {
            int level = item.getDefectLevel() == null ? 0 : item.getDefectLevel();
            Integer deduct = item.getDefectDeduct() != null ? item.getDefectDeduct() : LEVEL_DEDUCT[level];
            defects.add(new DefectEntry(item.getItemCode(), level, deduct));
        }
        return defects;
    }

    private void insertWithReportNoRetry(InspectionReport report) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                reportMapper.insert(report);
                return;
            } catch (DuplicateKeyException e) {
                report.setReportNo(nextReportNo());
            }
        }
        throw new BizException("报告编号生成失败，请重试");
    }

    private String nextReportNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "RP" + date + random;
    }
}
