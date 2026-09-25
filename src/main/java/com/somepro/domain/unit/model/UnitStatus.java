package com.somepro.domain.unit.model;

import com.somepro.common.exception.BizException;

/**
 * 使用单位状态：ACTIVE 在册 / CANCELLED 已注销。
 *
 * 落库存 {@link #name()}（PO 里是 VARCHAR），接口入参传英文码，
 * 非法码在 {@link #fromCode(String)} 统一拦成业务异常。
 */
public enum UnitStatus {

    ACTIVE,
    CANCELLED;

    /** 解析对外英文码；传 null 视为「没填」由调用方给默认值，无法识别时抛业务异常。 */
    public static UnitStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return UnitStatus.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException("单位状态只能是 ACTIVE（在册）或 CANCELLED（注销）：" + code);
        }
    }
}
