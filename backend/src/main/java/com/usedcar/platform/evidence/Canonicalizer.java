package com.usedcar.platform.evidence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.usedcar.platform.evidence.dto.DefectEntry;
import com.usedcar.platform.evidence.dto.ReportSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规范化器：把报告快照整理成唯一确定的字符串，保证同一份数据任何时候算出的哈希都相同。
 *
 * <p>规则（与设计文档 9.2 一致，改动必须同步升版本，否则历史报告将无法验真）：
 * <ol>
 *   <li>字段顺序固定，由代码中的 LinkedHashMap 写死，不依赖任何 Map 的遍历顺序</li>
 *   <li>编码 UTF-8，不做转义包装；字符串统一做 NFC 归一化并去除首尾空白</li>
 *   <li>空值统一：字符串 null 与空串都表示为 ""，数值 null 表示为 0</li>
 *   <li>数值统一：金额以“分”为单位的整数；里程、评分为整数；禁止浮点</li>
 *   <li>时间统一：UTC、精确到秒，格式 yyyy-MM-ddTHH:mm:ssZ</li>
 *   <li>缺陷列表按 itemCode、defectLevel、deduct 升序排序，避免录入顺序影响哈希</li>
 * </ol>
 */
@Component
public class Canonicalizer {

    /** 规范化版本号，写入报告以便未来兼容多版本 */
    public static final String VERSION = "v1";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.INDENT_OUTPUT);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

    public String canonicalize(ReportSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("报告快照不能为空");
        }
        if (snapshot.issuedAt() == null) {
            throw new IllegalArgumentException("出具时间不能为空");
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("reportNo", text(snapshot.reportNo()));
        root.put("vin", text(snapshot.vin()));
        root.put("mileage", number(snapshot.mileage()));
        root.put("totalScore", number(snapshot.totalScore()));
        root.put("grade", text(snapshot.grade()));
        root.put("defects", defects(snapshot.defects()));
        root.put("priceLow", number(snapshot.priceLow()));
        root.put("priceHigh", number(snapshot.priceHigh()));
        root.put("issuedAt", time(snapshot.issuedAt()));

        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("报告规范化失败", e);
        }
    }

    public String canonicalizeAndHash(ReportSnapshot snapshot) {
        return HashChain.sha256Hex(canonicalize(snapshot));
    }

    private List<Map<String, Object>> defects(List<DefectEntry> entries) {
        List<DefectEntry> source = entries == null ? List.of() : new ArrayList<>(entries);
        source.sort(Comparator
                .comparing((DefectEntry d) -> text(d.itemCode()))
                .thenComparing(d -> number(d.defectLevel()))
                .thenComparing(d -> number(d.deduct())));

        List<Map<String, Object>> result = new ArrayList<>(source.size());
        for (DefectEntry entry : source) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", text(entry.itemCode()));
            item.put("level", number(entry.defectLevel()));
            item.put("deduct", number(entry.deduct()));
            result.add(item);
        }
        return result;
    }

    private String text(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC).trim();
    }

    private long number(Number value) {
        return value == null ? 0L : value.longValue();
    }

    private String time(Instant instant) {
        return TIME_FORMAT.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }
}
