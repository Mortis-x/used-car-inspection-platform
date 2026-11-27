package com.usedcar.platform.evidence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.evidence.dto.ChainNode;
import com.usedcar.platform.evidence.dto.ChainVerification;
import com.usedcar.platform.evidence.dto.ReportVerification;
import com.usedcar.platform.report.InspectionReport;
import com.usedcar.platform.report.InspectionReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvidenceService {

    private final EvidenceChainMapper chainMapper;
    private final ChainHeadMapper headMapper;
    private final InspectionReportMapper reportMapper;

    public EvidenceService(EvidenceChainMapper chainMapper,
                           ChainHeadMapper headMapper,
                           InspectionReportMapper reportMapper) {
        this.chainMapper = chainMapper;
        this.headMapper = headMapper;
        this.reportMapper = reportMapper;
    }

    /**
     * 追加一条存证记录。必须在事务内调用：chain_head 行锁会一直持有到事务提交，
     * 从而保证并发情况下序号连续、前序哈希不断裂。
     */
    @Transactional
    public EvidenceChainRecord append(InspectionReport report, String reportHash, Instant issuedAt) {
        ChainHead head = headMapper.selectForUpdate();
        long seq = head.getSeqNo() + 1;
        String recordHash = HashChain.recordHash(head.getLastHash(), reportHash, issuedAt);

        EvidenceChainRecord record = new EvidenceChainRecord();
        record.setReportId(report.getId());
        record.setReportNo(report.getReportNo());
        record.setSeqNo(seq);
        record.setPrevHash(head.getLastHash());
        record.setReportHash(reportHash);
        record.setRecordHash(recordHash);
        record.setIssuedAt(LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()));
        chainMapper.insert(record);

        head.setSeqNo(seq);
        head.setLastHash(recordHash);
        headMapper.updateById(head);
        return record;
    }

    public List<EvidenceChainRecord> list() {
        return chainMapper.selectList(Wrappers.<EvidenceChainRecord>lambdaQuery()
                .orderByAsc(EvidenceChainRecord::getSeqNo));
    }

    public ChainHead head() {
        return headMapper.selectById(1);
    }

    public ChainVerification verifyAll() {
        List<ChainNode> nodes = new ArrayList<>();
        for (EvidenceChainRecord record : list()) {
            nodes.add(new ChainNode(
                    record.getSeqNo(),
                    record.getPrevHash(),
                    record.getReportHash(),
                    record.getRecordHash(),
                    toInstant(record.getIssuedAt())));
        }
        return HashChain.verify(nodes);
    }

    /**
     * 单份报告验真：重算规范化 JSON 的指纹，并与链上记录比对，再校验整条链。
     */
    public ReportVerification verifyReport(String reportNo) {
        EvidenceChainRecord record = chainMapper.selectOne(Wrappers.<EvidenceChainRecord>lambdaQuery()
                .eq(EvidenceChainRecord::getReportNo, reportNo)
                .last("limit 1"));
        if (record == null) {
            return ReportVerification.notFound(reportNo);
        }
        InspectionReport report = reportMapper.selectOne(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getReportNo, reportNo)
                .last("limit 1"));
        if (report == null || report.getCanonicalJson() == null) {
            return ReportVerification.fail(reportNo, "报告数据缺失，无法重算指纹");
        }

        String recomputed = HashChain.sha256Hex(report.getCanonicalJson());
        boolean hashMatched = recomputed.equals(record.getReportHash());
        ChainVerification chain = verifyAll();
        if (!hashMatched) {
            return ReportVerification.fail(reportNo, "报告内容与存证指纹不一致，数据可能被篡改");
        }
        if (!chain.valid()) {
            return ReportVerification.fail(reportNo, "存证链不完整：" + chain.reason());
        }
        return ReportVerification.pass(reportNo, report.getTotalScore(), report.getGrade(),
                report.getIssuedAt(), record.getSeqNo(), record.getRecordHash());
    }

    private Instant toInstant(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant();
    }
}
