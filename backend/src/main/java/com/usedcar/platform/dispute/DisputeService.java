package com.usedcar.platform.dispute;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.attachment.Attachment;
import com.usedcar.platform.attachment.AttachmentMapper;
import com.usedcar.platform.audit.AuditLogService;
import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.auth.Role;
import com.usedcar.platform.auth.UserPrincipal;
import com.usedcar.platform.common.BizException;
import com.usedcar.platform.dispute.dto.AddEvidenceRequest;
import com.usedcar.platform.dispute.dto.CreateDisputeRequest;
import com.usedcar.platform.dispute.dto.DisputeDetail;
import com.usedcar.platform.dispute.dto.DisputeRow;
import com.usedcar.platform.dispute.dto.ResolveDisputeRequest;
import com.usedcar.platform.evidence.EvidenceChainMapper;
import com.usedcar.platform.evidence.EvidenceChainRecord;
import com.usedcar.platform.evidence.VerifyRecord;
import com.usedcar.platform.evidence.VerifyRecordMapper;
import com.usedcar.platform.report.InspectionReport;
import com.usedcar.platform.report.InspectionReportMapper;
import com.usedcar.platform.vehicle.Vehicle;
import com.usedcar.platform.vehicle.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class DisputeService {

    private static final List<String> ARBITRATOR_ROLES = List.of("ARBITRATOR", "OPERATOR", "ADMIN");

    private final DisputeCaseMapper disputeCaseMapper;
    private final DisputeEvidenceMapper disputeEvidenceMapper;
    private final InspectionReportMapper reportMapper;
    private final VehicleMapper vehicleMapper;
    private final EvidenceChainMapper chainMapper;
    private final VerifyRecordMapper verifyRecordMapper;
    private final AttachmentMapper attachmentMapper;
    private final AuditLogService auditLogService;

    public DisputeService(DisputeCaseMapper disputeCaseMapper,
                          DisputeEvidenceMapper disputeEvidenceMapper,
                          InspectionReportMapper reportMapper,
                          VehicleMapper vehicleMapper,
                          EvidenceChainMapper chainMapper,
                          VerifyRecordMapper verifyRecordMapper,
                          AttachmentMapper attachmentMapper,
                          AuditLogService auditLogService) {
        this.disputeCaseMapper = disputeCaseMapper;
        this.disputeEvidenceMapper = disputeEvidenceMapper;
        this.reportMapper = reportMapper;
        this.vehicleMapper = vehicleMapper;
        this.chainMapper = chainMapper;
        this.verifyRecordMapper = verifyRecordMapper;
        this.attachmentMapper = attachmentMapper;
        this.auditLogService = auditLogService;
    }

    public List<DisputeRow> list(String status) {
        UserPrincipal principal = AuthContext.require();
        Long applicantId = ARBITRATOR_ROLES.contains(principal.role()) ? null : principal.id();
        return disputeCaseMapper.selectRows(status, applicantId);
    }

    @Transactional
    public DisputeCase create(CreateDisputeRequest request) {
        InspectionReport report = reportMapper.selectOne(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getReportNo, request.reportNo())
                .last("limit 1"));
        if (report == null || !"ACTIVE".equals(report.getStatus())) {
            throw new BizException("只有已生效报告可以发起纠纷举证");
        }
        Long duplicate = disputeCaseMapper.selectCount(Wrappers.<DisputeCase>lambdaQuery()
                .eq(DisputeCase::getReportId, report.getId())
                .eq(DisputeCase::getApplicantId, AuthContext.userId())
                .in(DisputeCase::getStatus, List.of("OPEN", "ARBITRATING")));
        if (duplicate != null && duplicate > 0) {
            throw new BizException("你对该报告已有进行中的纠纷案件");
        }

        UserPrincipal principal = AuthContext.require();
        DisputeCase dispute = new DisputeCase();
        dispute.setReportId(report.getId());
        dispute.setReportNo(report.getReportNo());
        dispute.setOrderId(report.getOrderId());
        dispute.setApplicantId(principal.id());
        dispute.setApplicantRole(principal.role());
        dispute.setReason(request.reason());
        dispute.setDescription(request.description());
        dispute.setStatus("OPEN");
        disputeCaseMapper.insert(dispute);

        // 自动把报告与链上存证纳入材料包，申请人无需手工上传
        addSystemEvidence(dispute, "REPORT", report.getId(),
                "检测报告 " + report.getReportNo(),
                "总分 " + report.getTotalScore() + "，等级 " + report.getGrade()
                        + "，版本 v" + report.getVersion() + "，出具时间 " + report.getIssuedAt());
        EvidenceChainRecord chain = chainMapper.selectOne(Wrappers.<EvidenceChainRecord>lambdaQuery()
                .eq(EvidenceChainRecord::getReportNo, report.getReportNo())
                .last("limit 1"));
        if (chain != null) {
            addSystemEvidence(dispute, "CHAIN", chain.getId(),
                    "存证链记录 #" + chain.getSeqNo(),
                    "report_hash=" + chain.getReportHash() + "\nrecord_hash=" + chain.getRecordHash());
        }

        auditLogService.record("CREATE", "DISPUTE", dispute.getId(),
                "对报告 " + report.getReportNo() + " 发起纠纷：" + request.reason());
        return dispute;
    }

    public DisputeDetail detail(Long id) {
        DisputeCase dispute = require(id);
        UserPrincipal principal = AuthContext.require();
        boolean allowed = Objects.equals(dispute.getApplicantId(), principal.id())
                || ARBITRATOR_ROLES.contains(principal.role());
        if (!allowed) {
            throw new BizException(403, "无权查看该纠纷案件");
        }
        List<DisputeEvidence> evidence = disputeEvidenceMapper.selectList(
                Wrappers.<DisputeEvidence>lambdaQuery()
                        .eq(DisputeEvidence::getDisputeId, id)
                        .orderByAsc(DisputeEvidence::getId));
        InspectionReport report = reportMapper.selectById(dispute.getReportId());
        Vehicle vehicle = report == null ? null : vehicleMapper.selectById(report.getVehicleId());
        EvidenceChainRecord chain = chainMapper.selectOne(Wrappers.<EvidenceChainRecord>lambdaQuery()
                .eq(EvidenceChainRecord::getReportNo, dispute.getReportNo())
                .last("limit 1"));
        Long verifyCount = verifyRecordMapper.selectCount(Wrappers.<VerifyRecord>lambdaQuery()
                .eq(VerifyRecord::getReportNo, dispute.getReportNo()));

        return new DisputeDetail(dispute, evidence,
                report == null ? null : new DisputeDetail.ReportSummary(
                        report.getReportNo(), report.getVersion(), report.getTotalScore(), report.getGrade(),
                        report.getStatus(), report.getIssuedAt(),
                        vehicle == null ? null : vehicle.getBrand(),
                        vehicle == null ? null : vehicle.getModel(),
                        vehicle == null ? null : vehicle.getPlateNo()),
                chain == null ? null : new DisputeDetail.ChainSummary(
                        chain.getSeqNo(), chain.getReportHash(), chain.getRecordHash(), chain.getPrevHash()),
                verifyCount == null ? 0 : verifyCount);
    }

    @Transactional
    public DisputeCase claim(Long id) {
        DisputeCase dispute = require(id);
        if (!"OPEN".equals(dispute.getStatus())) {
            throw new BizException("该案件已受理或已裁决");
        }
        dispute.setArbitratorId(AuthContext.userId());
        dispute.setStatus("ARBITRATING");
        disputeCaseMapper.updateById(dispute);
        auditLogService.record("UPDATE", "DISPUTE", id, "仲裁员受理纠纷案件");
        return dispute;
    }

    @Transactional
    public DisputeEvidence addEvidence(Long id, AddEvidenceRequest request) {
        DisputeCase dispute = require(id);
        UserPrincipal principal = AuthContext.require();
        boolean applicant = Objects.equals(dispute.getApplicantId(), principal.id());
        boolean arbitrator = ARBITRATOR_ROLES.contains(principal.role());
        if (!applicant && !arbitrator) {
            throw new BizException(403, "无权提交该案件材料");
        }
        if (List.of("RESOLVED", "CLOSED").contains(dispute.getStatus())) {
            throw new BizException("案件已裁决或已关闭，不能再补充材料");
        }

        String type = request.type().toUpperCase();
        if (!List.of("REPORT", "CHAIN", "VERIFY", "ATTACHMENT", "REMARK").contains(type)) {
            throw new BizException("不支持的材料类型");
        }
        String title;
        String content;
        Long refId = request.refId();

        switch (type) {
            case "REPORT" -> {
                InspectionReport report = reportMapper.selectById(dispute.getReportId());
                title = "检测报告 " + dispute.getReportNo();
                content = "总分 " + report.getTotalScore() + "，等级 " + report.getGrade()
                        + "，版本 v" + report.getVersion();
            }
            case "CHAIN" -> {
                EvidenceChainRecord chain = chainMapper.selectOne(Wrappers.<EvidenceChainRecord>lambdaQuery()
                        .eq(EvidenceChainRecord::getReportNo, dispute.getReportNo())
                        .last("limit 1"));
                if (chain == null) {
                    throw new BizException("该报告暂无存证记录");
                }
                refId = chain.getId();
                title = "存证链记录 #" + chain.getSeqNo();
                content = "report_hash=" + chain.getReportHash() + "\nrecord_hash=" + chain.getRecordHash();
            }
            case "VERIFY" -> {
                Long total = verifyRecordMapper.selectCount(Wrappers.<VerifyRecord>lambdaQuery()
                        .eq(VerifyRecord::getReportNo, dispute.getReportNo()));
                Long pass = verifyRecordMapper.selectCount(Wrappers.<VerifyRecord>lambdaQuery()
                        .eq(VerifyRecord::getReportNo, dispute.getReportNo())
                        .eq(VerifyRecord::getResult, "PASS"));
                List<VerifyRecord> latest = verifyRecordMapper.selectList(Wrappers.<VerifyRecord>lambdaQuery()
                        .eq(VerifyRecord::getReportNo, dispute.getReportNo())
                        .orderByDesc(VerifyRecord::getId)
                        .last("limit 1"));
                title = "验真记录汇总";
                content = "累计验真 " + total + " 次，通过 " + pass + " 次"
                        + (latest.isEmpty() ? "" : "；最近一次结果：" + latest.get(0).getResult());
            }
            case "ATTACHMENT" -> {
                if (refId == null) {
                    throw new BizException("请选择要引用的检测附件");
                }
                Attachment attachment = attachmentMapper.selectById(refId);
                if (attachment == null || !Objects.equals(attachment.getOrderId(), dispute.getOrderId())) {
                    throw new BizException("附件不属于该检测单");
                }
                title = "检测附件 " + attachment.getFileName();
                content = "原图指纹=" + attachment.getOriginHash()
                        + "\n状态=" + attachment.getStatus()
                        + "\n大小=" + attachment.getFileSize() + " 字节";
            }
            default -> {
                if (request.remark() == null || request.remark().isBlank()) {
                    throw new BizException("补充说明不能为空");
                }
                title = "补充说明";
                content = request.remark();
            }
        }

        DisputeEvidence evidence = new DisputeEvidence();
        evidence.setDisputeId(id);
        evidence.setType(type);
        evidence.setRefId(refId);
        evidence.setTitle(title);
        evidence.setContent(content);
        evidence.setSubmittedBy(principal.id());
        disputeEvidenceMapper.insert(evidence);
        auditLogService.record("UPDATE", "DISPUTE", id, "提交举证材料：" + title);
        return evidence;
    }

    @Transactional
    public DisputeCase resolve(Long id, ResolveDisputeRequest request) {
        DisputeCase dispute = require(id);
        if (List.of("RESOLVED", "CLOSED").contains(dispute.getStatus())) {
            throw new BizException("该案件已裁决或已关闭");
        }
        String result = request.result().toUpperCase();
        if (!Arrays.asList("SUPPORTED", "REJECTED").contains(result)) {
            throw new BizException("裁决结果只能是 SUPPORTED 或 REJECTED");
        }
        dispute.setStatus("RESOLVED");
        dispute.setResult(result);
        dispute.setConclusion(request.conclusion());
        dispute.setArbitratorId(AuthContext.userId());
        disputeCaseMapper.updateById(dispute);
        auditLogService.record("UPDATE", "DISPUTE", id,
                "裁决完成：" + ("SUPPORTED".equals(result) ? "支持申请人" : "驳回申请"));
        return dispute;
    }

    @Transactional
    public DisputeCase close(Long id) {
        DisputeCase dispute = require(id);
        UserPrincipal principal = AuthContext.require();
        boolean applicant = Objects.equals(dispute.getApplicantId(), principal.id());
        boolean arbitrator = ARBITRATOR_ROLES.contains(principal.role());
        if (!applicant && !arbitrator) {
            throw new BizException(403, "无权关闭该纠纷案件");
        }
        if (!List.of("OPEN", "ARBITRATING").contains(dispute.getStatus())) {
            throw new BizException("仅待受理或仲裁中的案件可以关闭");
        }
        dispute.setStatus("CLOSED");
        disputeCaseMapper.updateById(dispute);
        auditLogService.record("UPDATE", "DISPUTE", id,
                applicant ? "申请人撤回纠纷申请" : "仲裁员关闭纠纷案件");
        return dispute;
    }

    private void addSystemEvidence(DisputeCase dispute, String type, Long refId, String title, String content) {
        DisputeEvidence evidence = new DisputeEvidence();
        evidence.setDisputeId(dispute.getId());
        evidence.setType(type);
        evidence.setRefId(refId);
        evidence.setTitle(title);
        evidence.setContent(content);
        evidence.setSubmittedBy(dispute.getApplicantId());
        disputeEvidenceMapper.insert(evidence);
    }

    private DisputeCase require(Long id) {
        DisputeCase dispute = disputeCaseMapper.selectById(id);
        if (dispute == null) {
            throw new BizException(404, "纠纷案件不存在");
        }
        return dispute;
    }
}
