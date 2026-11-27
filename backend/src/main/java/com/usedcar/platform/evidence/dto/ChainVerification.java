package com.usedcar.platform.evidence.dto;

/**
 * 链校验结果。brokenSeqNo 为 null 表示整条链完整。
 */
public record ChainVerification(boolean valid, Long brokenSeqNo, String reason) {

    public static ChainVerification pass() {
        return new ChainVerification(true, null, "存证链完整");
    }

    public static ChainVerification fail(long seqNo, String reason) {
        return new ChainVerification(false, seqNo, reason);
    }
}
