package com.usedcar.platform.evidence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usedcar.platform.evidence.dto.DefectEntry;
import com.usedcar.platform.evidence.dto.FieldDiff;
import com.usedcar.platform.evidence.dto.PublicVerifyResult;
import com.usedcar.platform.evidence.dto.ReportSnapshot;
import com.usedcar.platform.inspection.InspectionItem;
import com.usedcar.platform.inspection.InspectionItemMapper;
import com.usedcar.platform.report.InspectionReport;
import com.usedcar.platform.report.InspectionReportMapper;
import com.usedcar.platform.vehicle.Vehicle;
import com.usedcar.platform.vehicle.VehicleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PublicVerifyService {

    private static final int[] LEVEL_DEDUCT = {0, 3, 6, 10};
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InspectionReportMapper reportMapper;
    private final VehicleMapper vehicleMapper;
    private final InspectionItemMapper itemMapper;
    private final EvidenceChainMapper chainMapper;
    private final VerifyRecordMapper verifyRecordMapper;
    private final EvidenceService evidenceService;
    private final Canonicalizer canonicalizer;

    public PublicVerifyService(InspectionReportMapper reportMapper,
                               VehicleMapper vehicleMapper,
                               InspectionItemMapper itemMapper,
                               EvidenceChainMapper chainMapper,
                               VerifyRecordMapper verifyRecordMapper,
                               EvidenceService evidenceService,
                               Canonicalizer canonicalizer) {
        this.reportMapper = reportMapper;
        this.vehicleMapper = vehicleMapper;
        this.itemMapper = itemMapper;
        this.chainMapper = chainMapper;
        this.verifyRecordMapper = verifyRecordMapper;
        this.evidenceService = evidenceService;
        this.canonicalizer = canonicalizer;
    }

    public PublicVerifyResult verify(String reportNo, String ip, String userAgent) {
        InspectionReport report = reportMapper.selectOne(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getReportNo, reportNo)
                .last("limit 1"));
        if (report == null) {
            return record(reportNo, ip, userAgent, PublicVerifyResultFactory.notFound(reportNo), "NOT_FOUND", null);
        }
        Vehicle vehicle = vehicleMapper.selectById(report.getVehicleId());
        EvidenceChainRecord chain = chainMapper.selectOne(Wrappers.<EvidenceChainRecord>lambdaQuery()
                .eq(EvidenceChainRecord::getReportNo, reportNo)
                .last("limit 1"));

        if (!"ACTIVE".equals(report.getStatus()) && !"SUPERSEDED".equals(report.getStatus())) {
            return record(reportNo, ip, userAgent,
                    PublicVerifyResultFactory.fail(report, vehicle, "报告未生效或已作废，不具备验真效力"),
                    "FAIL", null);
        }
        if (chain == null) {
            return record(reportNo, ip, userAgent,
                    PublicVerifyResultFactory.fail(report, vehicle, "未找到该报告的存证记录"),
                    "FAIL", null);
        }
        if (!evidenceService.verifyAll().valid()) {
            return record(reportNo, ip, userAgent,
                    PublicVerifyResultFactory.fail(report, vehicle, "存证链完整性校验未通过"),
                    "FAIL", null);
        }

        List<DefectEntry> defects = defectsOf(report.getOrderId());
        ReportSnapshot current = new ReportSnapshot(
                report.getReportNo(), vehicle.getVin(), vehicle.getMileage(),
                report.getTotalScore(), report.getGrade(), defects,
                report.getPriceLow(), report.getPriceHigh(),
                report.getIssuedAt().atZone(ZoneId.systemDefault()).toInstant());
        String currentCanonical = canonicalizer.canonicalize(current);
        String currentHash = HashChain.sha256Hex(currentCanonical);

        if (currentHash.equals(chain.getReportHash())) {
            return record(reportNo, ip, userAgent,
                    PublicVerifyResultFactory.pass(report, vehicle, chain, defects),
                    "PASS", null);
        }

        List<FieldDiff> diffs = diff(report.getCanonicalJson(), currentCanonical);
        PublicVerifyResult failed = PublicVerifyResultFactory.mismatch(report, vehicle, chain, defects, diffs);
        return record(reportNo, ip, userAgent, failed, "FAIL", diffs);
    }

    private PublicVerifyResult record(String reportNo, String ip, String userAgent,
                                      PublicVerifyResult result, String status, List<FieldDiff> diffs) {
        VerifyRecord record = new VerifyRecord();
        record.setReportNo(reportNo);
        record.setVerifyTime(LocalDateTime.now());
        record.setIp(ip == null ? null : ip.substring(0, Math.min(ip.length(), 64)));
        record.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255)));
        record.setResult(status);
        record.setDetail(result.message());
        if (diffs != null && !diffs.isEmpty()) {
            try {
                record.setDiffFields(MAPPER.writeValueAsString(diffs));
            } catch (Exception ignored) {
                // 明细序列化失败不影响验真结果
            }
        }
        verifyRecordMapper.insert(record);
        return result;
    }

    /**
     * 字段级对比：把库里当前数据重新规范化，与存证时的规范化 JSON 逐字段比对。
     */
    private List<FieldDiff> diff(String storedJson, String currentJson) {
        List<FieldDiff> diffs = new ArrayList<>();
        if (storedJson == null) {
            return diffs;
        }
        try {
            JsonNode stored = MAPPER.readTree(storedJson);
            JsonNode current = MAPPER.readTree(currentJson);
            String[] fields = {"reportNo", "vin", "mileage", "totalScore", "grade", "priceLow", "priceHigh", "issuedAt", "defects"};
            for (String field : fields) {
                String storedValue = text(stored.get(field));
                String currentValue = text(current.get(field));
                if (!storedValue.equals(currentValue)) {
                    diffs.add(new FieldDiff(labelOf(field), storedValue, currentValue));
                }
            }
        } catch (Exception e) {
            diffs.add(new FieldDiff("canonical", "解析失败", e.getMessage()));
        }
        return diffs;
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    private String labelOf(String field) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("reportNo", "报告编号");
        labels.put("vin", "车架号");
        labels.put("mileage", "表显里程");
        labels.put("totalScore", "总分");
        labels.put("grade", "车况等级");
        labels.put("priceLow", "估价下限");
        labels.put("priceHigh", "估价上限");
        labels.put("issuedAt", "出具时间");
        labels.put("defects", "缺陷明细");
        return labels.getOrDefault(field, field);
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

    /**
     * 统一构造返回对象，避免各处重复拼装。
     */
    private static final class PublicVerifyResultFactory {

        static PublicVerifyResult pass(InspectionReport report, Vehicle vehicle,
                                       EvidenceChainRecord chain, List<DefectEntry> defects) {
            return build(report, vehicle, true, "验真通过：报告内容与存证链一致",
                    chain.getSeqNo(), chain.getRecordHash(), defects, List.of());
        }

        static PublicVerifyResult fail(InspectionReport report, Vehicle vehicle, String message) {
            return build(report, vehicle, false, message, null, null, List.of(), List.of());
        }

        static PublicVerifyResult mismatch(InspectionReport report, Vehicle vehicle, EvidenceChainRecord chain,
                                           List<DefectEntry> defects, List<FieldDiff> diffs) {
            return build(report, vehicle, false, "报告内容与存证指纹不一致，以下字段可能被改动",
                    chain.getSeqNo(), chain.getRecordHash(), defects, diffs);
        }

        static PublicVerifyResult notFound(String reportNo) {
            return new PublicVerifyResult(reportNo, false, "未找到该报告，请核对报告编号",
                    null, null, null, null, null, null, null, null, null, null, List.of(), List.of());
        }

        private static PublicVerifyResult build(InspectionReport report, Vehicle vehicle,
                                                boolean valid, String message, Long seq, String recordHash,
                                                List<DefectEntry> defects, List<FieldDiff> diffs) {
            return new PublicVerifyResult(
                    report.getReportNo(), valid, message, report.getGrade(), report.getTotalScore(),
                    report.getVersion(), report.getIssuedAt(), seq, recordHash,
                    vehicle == null ? null : vehicle.getBrand(),
                    vehicle == null ? null : vehicle.getModel(),
                    vehicle == null ? null : maskPlate(vehicle.getPlateNo()),
                    vehicle == null ? null : vehicle.getMileage(),
                    defects, diffs);
        }

        private static String maskPlate(String plate) {
            if (plate == null || plate.length() < 4) {
                return plate;
            }
            return plate.substring(0, 2) + "***" + plate.substring(plate.length() - 2);
        }
    }
}
