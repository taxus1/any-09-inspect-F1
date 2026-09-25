-- 集成测试专用 H2 建表脚本（MySQL 兼容模式）。
-- 生产以 doc/schema/inspect.sql 为准；这里去掉 H2 不认的 ENGINE / CHARSET / 内联 KEY 语法，
-- 列与生产 DDL 一一对应（列名即契约）。

CREATE TABLE IF NOT EXISTS t_use_unit (
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
CREATE INDEX IF NOT EXISTS idx_unit_district ON t_use_unit (district);

CREATE TABLE IF NOT EXISTS t_equipment (
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
CREATE INDEX IF NOT EXISTS idx_equipment_unit ON t_equipment (unit_id);
CREATE INDEX IF NOT EXISTS idx_equipment_status ON t_equipment (status);
