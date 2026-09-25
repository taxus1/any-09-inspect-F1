package com.somepro.domain.equipment.model;

import com.somepro.common.exception.BizException;

/**
 * 设备状态（纯领域枚举，无框架注解）。
 * IN_USE 在用 / SUSPENDED 停用 / SEALED 封存 / SCRAPPED 已报废。
 *
 * 落库列存的是 {@link #name()}（见 EquipmentPoConverter）。
 */
public enum EquipmentStatus {

    IN_USE,
    SUSPENDED,
    SEALED,
    SCRAPPED;

    /** 按名称解析状态；非法值抛业务异常，给调用方明确的中文提示。 */
    public static EquipmentStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("设备状态不能为空：IN_USE / SUSPENDED / SEALED / SCRAPPED");
        }
        try {
            return EquipmentStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的设备状态：" + code
                    + "，只允许 IN_USE / SUSPENDED / SEALED / SCRAPPED");
        }
    }
}
