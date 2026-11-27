package com.usedcar.platform.evidence;

import com.usedcar.platform.evidence.dto.ChainNode;
import com.usedcar.platform.evidence.dto.ChainVerification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * 哈希链计算与校验。
 *
 * <pre>
 * report_hash      = SHA-256(规范化 JSON)
 * record_hash(n)   = SHA-256( prev_hash(n) + report_hash(n) + issued_at(n) )
 * prev_hash(1)     = 64 个 0（创世记录）
 * prev_hash(n)     = record_hash(n-1)
 * </pre>
 */
public final class HashChain {

    /** 创世记录的前序哈希：64 个 0 */
    public static final String GENESIS_PREV_HASH = "0".repeat(64);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private HashChain() {
    }

    public static String sha256Hex(String input) {
        if (input == null) {
            throw new IllegalArgumentException("待哈希内容不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    public static String recordHash(String prevHash, String reportHash, Instant issuedAt) {
        if (prevHash == null || prevHash.length() != 64) {
            throw new IllegalArgumentException("前序指纹必须为 64 位十六进制字符串");
        }
        if (reportHash == null || reportHash.length() != 64) {
            throw new IllegalArgumentException("报告指纹必须为 64 位十六进制字符串");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("出具时间不能为空");
        }
        String issuedAtText = TIME_FORMAT.format(issuedAt.truncatedTo(ChronoUnit.SECONDS));
        return sha256Hex(prevHash + reportHash + issuedAtText);
    }

    /**
     * 全链校验：序号连续、每条记录指纹可重算、前后指针首尾相接。
     */
    public static ChainVerification verify(List<ChainNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return ChainVerification.fail(0, "存证链为空");
        }
        List<ChainNode> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator.comparingLong(ChainNode::seqNo));

        String expectedPrev = GENESIS_PREV_HASH;
        long expectedSeq = 1;

        for (ChainNode node : sorted) {
            if (node.seqNo() != expectedSeq) {
                return ChainVerification.fail(node.seqNo(),
                        "链序号不连续，期望 " + expectedSeq + "，实际 " + node.seqNo());
            }
            if (!expectedPrev.equals(node.prevHash())) {
                return ChainVerification.fail(node.seqNo(),
                        "前序指纹断裂，期望 " + expectedPrev + "，实际 " + node.prevHash());
            }
            String recomputed = recordHash(node.prevHash(), node.reportHash(), node.issuedAt());
            if (!recomputed.equals(node.recordHash())) {
                return ChainVerification.fail(node.seqNo(),
                        "记录指纹与重算结果不一致，存证记录可能被篡改");
            }
            expectedPrev = node.recordHash();
            expectedSeq++;
        }
        return ChainVerification.pass();
    }
}
