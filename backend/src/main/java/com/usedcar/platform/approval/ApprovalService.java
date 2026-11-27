package com.usedcar.platform.approval;

import com.usedcar.platform.approval.dto.ApprovalRow;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalService {

    private final ApprovalTaskMapper approvalTaskMapper;

    public ApprovalService(ApprovalTaskMapper approvalTaskMapper) {
        this.approvalTaskMapper = approvalTaskMapper;
    }

    public List<ApprovalRow> list(String status) {
        return approvalTaskMapper.selectRows(status);
    }
}
