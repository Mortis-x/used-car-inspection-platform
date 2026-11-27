-- 更正申请需要携带拟修改的检测项，存为 JSON，审批通过后才落库
ALTER TABLE approval_task
    ADD COLUMN payload MEDIUMTEXT DEFAULT NULL COMMENT '审批载荷 JSON，更正申请为拟修改项列表' AFTER apply_reason;
