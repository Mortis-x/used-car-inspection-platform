-- =============================================================
-- 二手车检测报告与车况可信存证平台 —— 初始化脚本
-- 说明：
--   1. 存证相关表（evidence_chain / chain_head / verify_record）只允许追加
--   2. 所有金额字段以“分”为单位保存整数，禁止浮点
--   3. 所有时间字段统一保存为 Asia/Shanghai 时区的 DATETIME，存证时转为 UTC 秒
-- =============================================================

CREATE TABLE sys_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(50)  NOT NULL COMMENT '登录名',
    password     VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    real_name    VARCHAR(50)  NOT NULL COMMENT '姓名',
    role         VARCHAR(20)  NOT NULL COMMENT 'INSPECTOR/DEALER/BUYER/OPERATOR/ARBITRATOR/ADMIN',
    phone        VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    org_name     VARCHAR(100)          DEFAULT NULL COMMENT '所属机构',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '系统用户';

CREATE TABLE vehicle (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    vin            VARCHAR(17) NOT NULL COMMENT '车架号',
    plate_no       VARCHAR(20)          DEFAULT NULL COMMENT '车牌号',
    brand          VARCHAR(50) NOT NULL COMMENT '品牌',
    model          VARCHAR(50) NOT NULL COMMENT '车型',
    color          VARCHAR(20)          DEFAULT NULL,
    first_reg_date DATE                 DEFAULT NULL COMMENT '初次登记日期',
    mileage        INT         NOT NULL DEFAULT 0 COMMENT '表显里程(km)',
    transfer_count INT         NOT NULL DEFAULT 0 COMMENT '过户次数',
    dealer_id      BIGINT               DEFAULT NULL COMMENT '所属车商',
    created_by     BIGINT               DEFAULT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vehicle_vin (vin),
    KEY idx_vehicle_plate (plate_no),
    KEY idx_vehicle_dealer (dealer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '车辆档案';

CREATE TABLE maintenance_record (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    vehicle_id  BIGINT      NOT NULL,
    record_type VARCHAR(20) NOT NULL COMMENT 'MAINTAIN/REPAIR/ACCIDENT',
    record_date DATE        NOT NULL,
    mileage     INT                  DEFAULT NULL,
    provider    VARCHAR(100)         DEFAULT NULL COMMENT '维保机构',
    content     VARCHAR(500)         DEFAULT NULL,
    amount      BIGINT               DEFAULT NULL COMMENT '金额(分)',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_maintenance_vehicle (vehicle_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '维保记录';

CREATE TABLE inspection_template (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL COMMENT '模板名称',
    version    INT         NOT NULL DEFAULT 1,
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    remark     VARCHAR(500)         DEFAULT NULL,
    created_by BIGINT               DEFAULT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '检测模板';

CREATE TABLE inspection_template_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    template_id BIGINT       NOT NULL,
    category    VARCHAR(20)  NOT NULL COMMENT 'EXTERIOR/INTERIOR/ENGINE/CHASSIS/ACCIDENT/WATER_FIRE',
    item_code   VARCHAR(20)  NOT NULL,
    item_name   VARCHAR(100) NOT NULL,
    weight      INT          NOT NULL DEFAULT 1 COMMENT '权重',
    max_score   INT          NOT NULL DEFAULT 100 COMMENT '满分',
    defect_rule VARCHAR(500)          DEFAULT NULL COMMENT '扣分规则说明',
    sort        INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_item (template_id, item_code),
    KEY idx_template_item_category (template_id, category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '模板检测项';

CREATE TABLE inspection_order (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(32) NOT NULL COMMENT '检测单号',
    vehicle_id     BIGINT      NOT NULL,
    template_id    BIGINT      NOT NULL,
    inspector_id   BIGINT               DEFAULT NULL COMMENT '检测师',
    dealer_id      BIGINT               DEFAULT NULL COMMENT '委托车商',
    status         VARCHAR(20) NOT NULL COMMENT 'PENDING/INSPECTING/PENDING_REPORT/PENDING_APPROVAL/ISSUED/WITHDRAWN',
    remark         VARCHAR(500)         DEFAULT NULL,
    withdraw_reason VARCHAR(500)        DEFAULT NULL,
    submitted_at   DATETIME             DEFAULT NULL,
    issued_at      DATETIME             DEFAULT NULL,
    created_by     BIGINT               DEFAULT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_vehicle (vehicle_id),
    KEY idx_order_status (status),
    KEY idx_order_inspector (inspector_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '检测单';

CREATE TABLE inspection_item (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_id         BIGINT       NOT NULL,
    template_item_id BIGINT       NOT NULL,
    category         VARCHAR(20)  NOT NULL,
    item_code        VARCHAR(20)  NOT NULL,
    item_name        VARCHAR(100) NOT NULL,
    score            INT                   DEFAULT NULL COMMENT '0-100，NULL 表示未填',
    defect_level     TINYINT               DEFAULT NULL COMMENT '0无 1轻微 2一般 3严重，NULL 表示未填',
    defect_deduct    INT                   DEFAULT NULL COMMENT '缺陷扣减(分)',
    remark           VARCHAR(500)          DEFAULT NULL,
    sort             INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_order_code (order_id, item_code),
    KEY idx_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '检测项结果';

CREATE TABLE attachment (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_id     BIGINT       NOT NULL,
    item_id      BIGINT                DEFAULT NULL COMMENT '关联检测项，可为空',
    file_name    VARCHAR(200) NOT NULL,
    origin_path  VARCHAR(255)          DEFAULT NULL COMMENT '对象存储原始路径',
    thumb_path   VARCHAR(255)          DEFAULT NULL COMMENT '缩略图路径',
    origin_hash  CHAR(64)              DEFAULT NULL COMMENT '原图 SHA-256',
    content_type VARCHAR(100)          DEFAULT NULL,
    file_size    BIGINT                DEFAULT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/DONE/FAILED',
    retry_count  INT          NOT NULL DEFAULT 0,
    error_msg    VARCHAR(500)          DEFAULT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_attachment_order (order_id),
    KEY idx_attachment_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '检测附件';

CREATE TABLE inspection_report (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    report_no      VARCHAR(32)  NOT NULL COMMENT '报告编号，不暴露自增 id',
    order_id       BIGINT       NOT NULL,
    vehicle_id     BIGINT       NOT NULL,
    version        INT          NOT NULL DEFAULT 1,
    total_score    INT          NOT NULL,
    grade          CHAR(1)      NOT NULL COMMENT 'S/A/B/C/D',
    price_low      BIGINT       NOT NULL COMMENT '估价下限(分)',
    price_high     BIGINT       NOT NULL COMMENT '估价上限(分)',
    canonical_json MEDIUMTEXT            DEFAULT NULL COMMENT '规范化后的存证对象',
    pdf_path       VARCHAR(255)          DEFAULT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SUPERSEDED/VOID',
    issued_at      DATETIME     NOT NULL,
    created_by     BIGINT                DEFAULT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_no (report_no),
    UNIQUE KEY uk_report_order_version (order_id, version),
    KEY idx_report_vehicle (vehicle_id),
    KEY idx_report_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '检测报告';

-- 存证链：只允许 INSERT，任何业务代码都不得 UPDATE / DELETE
CREATE TABLE evidence_chain (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    report_id   BIGINT      NOT NULL,
    report_no   VARCHAR(32) NOT NULL,
    seq_no      BIGINT      NOT NULL COMMENT '链上序号，从 1 开始',
    prev_hash   CHAR(64)    NOT NULL COMMENT '前一条记录的 record_hash',
    report_hash CHAR(64)    NOT NULL COMMENT '报告结构化数据的 SHA-256',
    record_hash CHAR(64)    NOT NULL COMMENT 'SHA-256(prev_hash + report_hash + issued_at)',
    issued_at   DATETIME    NOT NULL COMMENT '报告出具时间',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_evidence_report (report_id),
    UNIQUE KEY uk_evidence_seq (seq_no),
    KEY idx_evidence_report_no (report_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '哈希链存证';

-- 链头指针：单行表，写入时 SELECT ... FOR UPDATE 保证串行
CREATE TABLE chain_head (
    id         TINYINT  NOT NULL COMMENT '固定为 1',
    seq_no     BIGINT   NOT NULL DEFAULT 0,
    last_hash  CHAR(64) NOT NULL COMMENT '最新一条 record_hash，创世值为 64 个 0',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '哈希链头指针';

INSERT INTO chain_head (id, seq_no, last_hash)
VALUES (1, 0, '0000000000000000000000000000000000000000000000000000000000000000');

CREATE TABLE verify_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    report_no   VARCHAR(32)  NOT NULL,
    verify_time DATETIME     NOT NULL,
    ip          VARCHAR(64)           DEFAULT NULL,
    user_agent  VARCHAR(255)          DEFAULT NULL,
    result      VARCHAR(20)  NOT NULL COMMENT 'PASS/FAIL/NOT_FOUND',
    diff_fields TEXT                  DEFAULT NULL COMMENT '不一致字段明细',
    detail      TEXT                  DEFAULT NULL COMMENT '验真过程说明',
    PRIMARY KEY (id),
    KEY idx_verify_report_no (report_no),
    KEY idx_verify_time (verify_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '验真记录';

CREATE TABLE intention_order (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    report_id         BIGINT      NOT NULL,
    buyer_id          BIGINT      NOT NULL,
    offer_price       BIGINT      NOT NULL COMMENT '出价(分)',
    dealer_reply_price BIGINT              DEFAULT NULL COMMENT '车商还价(分)',
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/COUNTERED/CANCELLED',
    message           VARCHAR(500)         DEFAULT NULL,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_intention_report (report_id),
    KEY idx_intention_buyer (buyer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '买家意向单';

CREATE TABLE approval_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    biz_type        VARCHAR(20)  NOT NULL COMMENT 'REPORT/CORRECTION',
    biz_id          BIGINT                DEFAULT NULL,
    order_id        BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    apply_user_id   BIGINT       NOT NULL,
    apply_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    apply_reason    VARCHAR(500)          DEFAULT NULL,
    approve_user_id BIGINT                DEFAULT NULL,
    approve_time    DATETIME              DEFAULT NULL,
    opinion         VARCHAR(500)          DEFAULT NULL COMMENT '审批意见',
    PRIMARY KEY (id),
    KEY idx_approval_order (order_id),
    KEY idx_approval_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '关键节点审批任务';

CREATE TABLE audit_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT                DEFAULT NULL,
    username   VARCHAR(50)           DEFAULT NULL,
    role       VARCHAR(20)           DEFAULT NULL,
    action     VARCHAR(50)  NOT NULL COMMENT 'LOGIN/CREATE/UPDATE/WITHDRAW/APPROVE/ISSUE/VERIFY/EXPORT',
    biz_type   VARCHAR(30)           DEFAULT NULL COMMENT 'VEHICLE/ORDER/REPORT/EVIDENCE/INTENTION',
    biz_id     BIGINT                DEFAULT NULL,
    detail     TEXT                   DEFAULT NULL COMMENT '变更前后值、失败原因等',
    ip         VARCHAR(64)           DEFAULT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_biz (biz_type, biz_id),
    KEY idx_audit_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '审计日志';
