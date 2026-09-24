-- any-09-inspect · 特种设备法定定期检验与隐患整改闭环 · 建表 SQL
-- 字符集 utf8mb4，时区 Asia/Shanghai。create 阶段建好，模型只写业务代码，不碰建表。
-- 列名即契约：del_flag 由 @TableLogic 自动拼接（查询带 del_flag=0，删除置 1），
-- create_by/update_by/create_time/update_time 由 AutoFillMetaObjectHandler 自动填充，业务代码不要手写。
-- 主键 id 由应用侧雪花分配（IdType.INPUT），不依赖自增。

-- 1) 使用单位档案（设备挂在单位下）
CREATE TABLE IF NOT EXISTS t_use_unit (
    id           BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    unit_code    VARCHAR(32)  NOT NULL COMMENT '使用单位编号，全局唯一（如 SY-0031）',
    unit_name    VARCHAR(128) NOT NULL COMMENT '单位名称',
    credit_code  VARCHAR(32)  DEFAULT NULL COMMENT '统一社会信用代码',
    district     VARCHAR(64)  DEFAULT NULL COMMENT '所属区县',
    contact_name VARCHAR(64)  DEFAULT NULL COMMENT '安全管理员姓名',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 在册 / CANCELLED 已注销',
    del_flag     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64)  DEFAULT NULL,
    create_time  DATETIME     DEFAULT NULL,
    update_by    VARCHAR(64)  DEFAULT NULL,
    update_time  DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_unit_code (unit_code),
    KEY idx_district (district)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特种设备使用单位档案';

-- 2) 设备档案（一台设备归属一个使用单位）
CREATE TABLE IF NOT EXISTS t_equipment (
    id             BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    reg_code       VARCHAR(32) NOT NULL COMMENT '设备注册代码，全局唯一（如 SB-2026-0031）',
    device_type    VARCHAR(24) NOT NULL COMMENT 'ELEVATOR 电梯 / BOILER 锅炉 / PRESSURE_VESSEL 压力容器 / CRANE 起重机械',
    unit_id        BIGINT      NOT NULL COMMENT '使用单位 id（t_use_unit.id）',
    install_addr   VARCHAR(255) DEFAULT NULL COMMENT '安装地点',
    commission_date DATE       DEFAULT NULL COMMENT '投用日期',
    period_month   INT         NOT NULL DEFAULT 12 COMMENT '法定检验周期（月）',
    next_inspect_date DATE     DEFAULT NULL COMMENT '下次检验到期日',
    status         VARCHAR(16) NOT NULL DEFAULT 'IN_USE' COMMENT 'IN_USE 在用 / SUSPENDED 停用 / SEALED 封存 / SCRAPPED 已报废',
    del_flag       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by      VARCHAR(64) DEFAULT NULL,
    create_time    DATETIME    DEFAULT NULL,
    update_by      VARCHAR(64) DEFAULT NULL,
    update_time    DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_reg_code (reg_code),
    KEY idx_unit (unit_id),
    KEY idx_next (next_inspect_date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特种设备档案';

-- 3) 检验人员（资质类别决定能检哪些设备；同一天派单量受上限约束）
CREATE TABLE IF NOT EXISTS t_inspector (
    id            BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    inspector_code VARCHAR(32) NOT NULL COMMENT '检验人员编号，全局唯一（如 JY-007）',
    full_name     VARCHAR(64) NOT NULL COMMENT '姓名',
    cert_type     VARCHAR(64) NOT NULL COMMENT '可检设备类别，多个用逗号分隔（ELEVATOR,BOILER 等）',
    cert_expire   DATE        DEFAULT NULL COMMENT '检验资格证有效期至',
    daily_limit   INT         NOT NULL DEFAULT 3 COMMENT '同一天可承接任务上限',
    status        VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 可派 / SUSPENDED 暂停派工 / EXPIRED 资格失效',
    del_flag      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by     VARCHAR(64) DEFAULT NULL,
    create_time   DATETIME    DEFAULT NULL,
    update_by     VARCHAR(64) DEFAULT NULL,
    update_time   DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_inspector_code (inspector_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验人员档案';

-- 4) 检验任务（一台设备一次到期检验，派给一名检验员）
CREATE TABLE IF NOT EXISTS t_inspect_task (
    id           BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    task_no      VARCHAR(32) NOT NULL COMMENT '任务编号，全局唯一（如 RW-2026-0001）',
    equipment_id BIGINT      NOT NULL COMMENT '设备 id（t_equipment.id）',
    inspector_id BIGINT      DEFAULT NULL COMMENT '检验人员 id（t_inspector.id）',
    plan_date    DATE        NOT NULL COMMENT '计划检验日期',
    status       VARCHAR(16) NOT NULL DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED 已派工 / INSPECTING 检验中 / CLOSED 已出结论 / VOID 已作废',
    void_reason  VARCHAR(255) DEFAULT NULL COMMENT '作废原因',
    del_flag     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64) DEFAULT NULL,
    create_time  DATETIME    DEFAULT NULL,
    update_by    VARCHAR(64) DEFAULT NULL,
    update_time  DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_equipment (equipment_id),
    KEY idx_inspector (inspector_id),
    KEY idx_plan_date (plan_date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定期检验任务';

-- 5) 检验结论与报告（一条任务一份结论）
CREATE TABLE IF NOT EXISTS t_inspect_result (
    id               BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    task_id          BIGINT      NOT NULL COMMENT '检验任务 id（t_inspect_task.id）',
    equipment_id     BIGINT      NOT NULL COMMENT '设备 id（t_equipment.id）',
    conclusion       VARCHAR(16) NOT NULL COMMENT 'PASS 合格 / RECTIFY 限期整改 / STOP_USE 停用',
    problem_desc     VARCHAR(255) DEFAULT NULL COMMENT '发现问题描述',
    inspect_date     DATE        DEFAULT NULL COMMENT '实际检验日期',
    next_inspect_date DATE       DEFAULT NULL COMMENT '本次结论推算出的下次检验到期日',
    report_no        VARCHAR(32) DEFAULT NULL COMMENT '报告编号（如 BG-2026-0001）',
    report_status    VARCHAR(16) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED 已出具 / REVOKED 已撤回',
    report_by        VARCHAR(64) DEFAULT NULL COMMENT '出具人',
    report_time      DATETIME    DEFAULT NULL COMMENT '出具时刻',
    del_flag         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by        VARCHAR(64) DEFAULT NULL,
    create_time      DATETIME    DEFAULT NULL,
    update_by        VARCHAR(64) DEFAULT NULL,
    update_time      DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_task (task_id),
    KEY idx_equipment (equipment_id),
    KEY idx_report_no (report_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验结论与报告';

-- 6) 隐患登记与整改闭环（一份结论下可有多条隐患）
CREATE TABLE IF NOT EXISTS t_defect (
    id              BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    result_id       BIGINT      NOT NULL COMMENT '检验结论 id（t_inspect_result.id）',
    equipment_id    BIGINT      NOT NULL COMMENT '设备 id（t_equipment.id）',
    defect_level    VARCHAR(16) NOT NULL COMMENT 'MINOR 一般 / MAJOR 严重 / SERIOUS 重大',
    defect_desc     VARCHAR(255) DEFAULT NULL COMMENT '隐患描述',
    rectify_deadline DATE       DEFAULT NULL COMMENT '整改期限（含当日）',
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN 待整改 / RECTIFIED 已整改待验收 / VERIFIED 已验收 / OVERDUE 已超期',
    rectify_result  VARCHAR(255) DEFAULT NULL COMMENT '整改情况说明',
    verify_by       VARCHAR(64) DEFAULT NULL COMMENT '验收人',
    verify_time     DATETIME    DEFAULT NULL COMMENT '验收时刻',
    del_flag        TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by       VARCHAR(64) DEFAULT NULL,
    create_time     DATETIME    DEFAULT NULL,
    update_by       VARCHAR(64) DEFAULT NULL,
    update_time     DATETIME    DEFAULT NULL,
    KEY idx_result (result_id),
    KEY idx_equipment (equipment_id),
    KEY idx_status (status),
    KEY idx_deadline (rectify_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患登记与整改';

-- 7) 设备状态流水（停用 / 恢复 / 封存 / 报废的动作留痕）
CREATE TABLE IF NOT EXISTS t_equipment_event (
    id           BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    equipment_id BIGINT      NOT NULL COMMENT '设备 id（t_equipment.id）',
    event_type   VARCHAR(16) NOT NULL COMMENT 'STOP 停用 / RESUME 恢复 / SEAL 封存 / SCRAP 报废',
    reason       VARCHAR(255) DEFAULT NULL COMMENT '事由',
    event_time   DATETIME    DEFAULT NULL COMMENT '发生时刻',
    del_flag     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64) DEFAULT NULL,
    create_time  DATETIME    DEFAULT NULL,
    update_by    VARCHAR(64) DEFAULT NULL,
    update_time  DATETIME    DEFAULT NULL,
    KEY idx_equipment (equipment_id),
    KEY idx_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态流水';
