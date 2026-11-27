-- 纠纷举证：案件 + 材料
CREATE TABLE dispute_case (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    report_id      BIGINT      NOT NULL,
    report_no      VARCHAR(32) NOT NULL,
    order_id       BIGINT      NOT NULL,
    applicant_id   BIGINT      NOT NULL,
    applicant_role VARCHAR(20) NOT NULL,
    reason         VARCHAR(200) NOT NULL,
    description    TEXT                 DEFAULT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ARBITRATING/RESOLVED/CLOSED',
    arbitrator_id  BIGINT               DEFAULT NULL,
    result         VARCHAR(20)          DEFAULT NULL COMMENT 'SUPPORTED/REJECTED',
    conclusion     TEXT                 DEFAULT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_dispute_report (report_no),
    KEY idx_dispute_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '纠纷案件';

CREATE TABLE dispute_evidence (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    dispute_id   BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL COMMENT 'REPORT/CHAIN/VERIFY/ATTACHMENT/REMARK',
    ref_id       BIGINT                DEFAULT NULL,
    title        VARCHAR(200)          DEFAULT NULL,
    content      TEXT                  DEFAULT NULL,
    submitted_by BIGINT       NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_evidence_dispute (dispute_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '纠纷举证材料';
