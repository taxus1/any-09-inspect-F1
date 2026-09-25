package com.somepro.domain.useunit.model;

import com.somepro.common.exception.BizException;

/**
 * 使用单位状态（纯领域枚举，无框架注解）。
 * ACTIVE 在册 / CANCELLED 已注销。
 *
 * 落库列存的是 {@link #name()}（见 UseUnitPoConverter），不依赖 MyBatis 的枚举处理。
 */
public enum UnitStatus {

    ACTIVE,
    CANCELLED;

    /** 按名称解析状态；非法值抛业务异常，给调用方明确的中文提示。 */
    public static UnitStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("单位状态不能为空：ACTIVE 在册 / CANCELLED 注销");
        }
        try {
            return UnitStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的单位状态：" + code + "，只允许 ACTIVE / CANCELLED");
        }
    }
}
