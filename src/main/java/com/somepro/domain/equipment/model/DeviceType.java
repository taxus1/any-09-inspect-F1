package com.somepro.domain.equipment.model;

import com.somepro.common.exception.BizException;

/**
 * 特种设备类别（纯领域枚举，无框架注解）。
 * ELEVATOR 电梯 / BOILER 锅炉 / PRESSURE_VESSEL 压力容器 / CRANE 起重机械。
 *
 * 落库列存的是 {@link #name()}（见 EquipmentPoConverter）。
 */
public enum DeviceType {

    ELEVATOR,
    BOILER,
    PRESSURE_VESSEL,
    CRANE;

    /** 按名称解析类别；非法值抛业务异常，给调用方明确的中文提示。 */
    public static DeviceType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("设备类别不能为空：ELEVATOR / BOILER / PRESSURE_VESSEL / CRANE");
        }
        try {
            return DeviceType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的设备类别：" + code
                    + "，只允许 ELEVATOR / BOILER / PRESSURE_VESSEL / CRANE");
        }
    }
}
