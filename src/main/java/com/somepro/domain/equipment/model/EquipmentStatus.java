package com.somepro.domain.equipment.model;

import com.somepro.common.exception.BizException;

/**
 * 设备状态：IN_USE 在用 / SUSPENDED 停用 / SEALED 封存 / SCRAPPED 已报废。
 */
public enum EquipmentStatus {

    IN_USE,
    SUSPENDED,
    SEALED,
    SCRAPPED;

    public static EquipmentStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return EquipmentStatus.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException("设备状态只能是 IN_USE / SUSPENDED / SEALED / SCRAPPED：" + code);
        }
    }
}
