package com.usedcar.platform.evidence;

import com.usedcar.platform.evidence.dto.DefectEntry;
import com.usedcar.platform.evidence.dto.ReportSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalizerTest {

    private final Canonicalizer canonicalizer = new Canonicalizer();

    private static final Instant ISSUED_AT = Instant.parse("2025-03-12T08:20:03Z");

    @Test
    @DisplayName("同一份数据多次规范化，哈希完全一致")
    void sameDataProducesSameHash() {
        String first = canonicalizer.canonicalizeAndHash(snapshot(68000, List.of(
                new DefectEntry("EXT-03", 2, 300),
                new DefectEntry("EXT-07", 1, 200))));
        String second = canonicalizer.canonicalizeAndHash(snapshot(68000, List.of(
                new DefectEntry("EXT-03", 2, 300),
                new DefectEntry("EXT-07", 1, 200))));

        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    @DisplayName("缺陷录入顺序不同，不影响哈希")
    void defectOrderDoesNotAffectHash() {
        String ordered = canonicalizer.canonicalizeAndHash(snapshot(68000, List.of(
                new DefectEntry("EXT-03", 2, 300),
                new DefectEntry("ENG-01", 3, 800))));
        String reversed = canonicalizer.canonicalizeAndHash(snapshot(68000, List.of(
                new DefectEntry("ENG-01", 3, 800),
                new DefectEntry("EXT-03", 2, 300))));

        assertThat(ordered).isEqualTo(reversed);
    }

    @Test
    @DisplayName("空值与空串规范化后一致")
    void nullAndBlankAreEquivalent() {
        ReportSnapshot withNull = new ReportSnapshot("RPT-1", null, 100, 90, null,
                List.of(), 100L, 200L, ISSUED_AT);
        ReportSnapshot withBlank = new ReportSnapshot("RPT-1", "  ", 100, 90, "",
                List.of(), 100L, 200L, ISSUED_AT);

        assertThat(canonicalizer.canonicalize(withNull))
                .isEqualTo(canonicalizer.canonicalize(withBlank));
    }

    @Test
    @DisplayName("时间精确到秒，毫秒差异不影响哈希")
    void timeIsTruncatedToSeconds() {
        ReportSnapshot withMillis = new ReportSnapshot("RPT-1", "VIN1", 100, 90, "B",
                List.of(), 100L, 200L, Instant.parse("2025-03-12T08:20:03.987Z"));
        ReportSnapshot withoutMillis = new ReportSnapshot("RPT-1", "VIN1", 100, 90, "B",
                List.of(), 100L, 200L, Instant.parse("2025-03-12T08:20:03Z"));

        assertThat(canonicalizer.canonicalizeAndHash(withMillis))
                .isEqualTo(canonicalizer.canonicalizeAndHash(withoutMillis));
    }

    @Test
    @DisplayName("任意纳入存证的字段被修改，哈希都会变化")
    void anyFieldChangeBreaksHash() {
        String base = canonicalizer.canonicalizeAndHash(snapshot(68000, List.of(
                new DefectEntry("EXT-03", 2, 300))));

        String mileageChanged = canonicalizer.canonicalizeAndHash(snapshot(68001, List.of(
                new DefectEntry("EXT-03", 2, 300))));
        String defectChanged = canonicalizer.canonicalizeAndHash(snapshot(68000, List.of(
                new DefectEntry("EXT-03", 3, 300))));

        assertThat(mileageChanged).isNotEqualTo(base);
        assertThat(defectChanged).isNotEqualTo(base);
    }

    @Test
    @DisplayName("规范化 JSON 的键顺序固定，便于人工比对")
    void keyOrderIsFixed() {
        String json = canonicalizer.canonicalize(snapshot(68000, List.of()));

        assertThat(json).startsWith("{\"reportNo\":\"RPT-20250312-0087\",\"vin\":\"LBV12345678901234\",");
        assertThat(json).contains("\"issuedAt\":\"2025-03-12T08:20:03Z\"}");
    }

    private ReportSnapshot snapshot(int mileage, List<DefectEntry> defects) {
        return new ReportSnapshot(
                "RPT-20250312-0087",
                "LBV12345678901234",
                mileage,
                82,
                "B",
                defects,
                1520000L,
                1640000L,
                ISSUED_AT);
    }
}
