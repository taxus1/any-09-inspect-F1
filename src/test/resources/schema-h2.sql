-- 切片测试建表（H2，MySQL 兼容模式）：形状对齐 doc/schema/inspect.sql 的 t_use_unit / t_equipment
CREATE TABLE t_use_unit (
    id            BIGINT       NOT NULL PRIMARY KEY,
    unit_code     VARCHAR(32)  NOT NULL,
    unit_name     VARCHAR(128) NOT NULL,
    credit_code   VARCHAR(32)  DEFAULT NULL,
    district      VARCHAR(64)  DEFAULT NULL,
    contact_name  VARCHAR(64)  DEFAULT NULL,
    contact_phone VARCHAR(32)  DEFAULT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    del_flag      TINYINT      NOT NULL DEFAULT 0,
    create_by     VARCHAR(64)  DEFAULT NULL,
    create_time   DATETIME     DEFAULT NULL,
    update_by     VARCHAR(64)  DEFAULT NULL,
    update_time   DATETIME     DEFAULT NULL,
    CONSTRAINT uk_unit_code UNIQUE (unit_code)
);

CREATE TABLE t_equipment (
    id                BIGINT      NOT NULL PRIMARY KEY,
    reg_code          VARCHAR(32) NOT NULL,
    device_type       VARCHAR(24) NOT NULL,
    unit_id           BIGINT      NOT NULL,
    install_addr      VARCHAR(255) DEFAULT NULL,
    commission_date   DATE        DEFAULT NULL,
    period_month      INT         NOT NULL DEFAULT 12,
    next_inspect_date DATE        DEFAULT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'IN_USE',
    del_flag          TINYINT     NOT NULL DEFAULT 0,
    create_by         VARCHAR(64) DEFAULT NULL,
    create_time       DATETIME    DEFAULT NULL,
    update_by         VARCHAR(64) DEFAULT NULL,
    update_time       DATETIME    DEFAULT NULL,
    CONSTRAINT uk_reg_code UNIQUE (reg_code)
);
