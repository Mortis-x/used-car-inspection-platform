package com.usedcar.platform.evidence;

import com.usedcar.platform.evidence.dto.ChainNode;
import com.usedcar.platform.evidence.dto.ChainVerification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HashChainTest {

    @Test
    @DisplayName("创世记录的前序指纹为 64 个 0")
    void genesisPrevHash() {
        assertThat(HashChain.GENESIS_PREV_HASH).hasSize(64).matches("0{64}");
    }

    @Test
    @DisplayName("三条记录首尾相接，全链校验通过")
    void validChainPasses() {
        List<ChainNode> nodes = buildChain(3);

        ChainVerification result = HashChain.verify(nodes);

        assertThat(result.valid()).isTrue();
        assertThat(result.brokenSeqNo()).isNull();
    }

    @Test
    @DisplayName("篡改中间记录的报告指纹，校验在第 2 条中断")
    void tamperedReportHashIsDetected() {
        List<ChainNode> nodes = new ArrayList<>(buildChain(3));
        ChainNode origin = nodes.get(1);
        nodes.set(1, new ChainNode(origin.seqNo(), origin.prevHash(),
                "f".repeat(64), origin.recordHash(), origin.issuedAt()));

        ChainVerification result = HashChain.verify(nodes);

        assertThat(result.valid()).isFalse();
        assertThat(result.brokenSeqNo()).isEqualTo(2L);
        assertThat(result.reason()).contains("记录指纹");
    }

    @Test
    @DisplayName("断开前后指针，校验在第 3 条中断")
    void brokenPointerIsDetected() {
        List<ChainNode> nodes = new ArrayList<>(buildChain(3));
        ChainNode third = nodes.get(2);
        nodes.set(2, new ChainNode(third.seqNo(), "a".repeat(64),
                third.reportHash(), third.recordHash(), third.issuedAt()));

        ChainVerification result = HashChain.verify(nodes);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("前序指纹断裂");
    }

    @Test
    @DisplayName("中间记录被删除导致序号不连续，校验失败")
    void missingRecordIsDetected() {
        List<ChainNode> nodes = buildChain(3);
        nodes.remove(1);

        ChainVerification result = HashChain.verify(nodes);

        assertThat(result.valid()).isFalse();
        assertThat(result.brokenSeqNo()).isEqualTo(3L);
        assertThat(result.reason()).contains("序号不连续");
    }

    @Test
    @DisplayName("记录指纹由前序指纹、报告指纹、出具时间共同决定")
    void recordHashDependsOnAllInputs() {
        String prev = HashChain.GENESIS_PREV_HASH;
        String report = "b".repeat(64);
        Instant time = Instant.parse("2025-03-12T08:20:03Z");

        String base = HashChain.recordHash(prev, report, time);

        assertThat(HashChain.recordHash("c".repeat(64), report, time)).isNotEqualTo(base);
        assertThat(HashChain.recordHash(prev, "d".repeat(64), time)).isNotEqualTo(base);
        assertThat(HashChain.recordHash(prev, report, time.plusSeconds(1))).isNotEqualTo(base);
    }

    private List<ChainNode> buildChain(int size) {
        List<ChainNode> nodes = new ArrayList<>();
        String prev = HashChain.GENESIS_PREV_HASH;
        for (int i = 1; i <= size; i++) {
            String reportHash = HashChain.sha256Hex("report-" + i);
            Instant issuedAt = Instant.parse("2025-03-12T08:20:0" + i + "Z");
            String recordHash = HashChain.recordHash(prev, reportHash, issuedAt);
            nodes.add(new ChainNode(i, prev, reportHash, recordHash, issuedAt));
            prev = recordHash;
        }
        return nodes;
    }
}
