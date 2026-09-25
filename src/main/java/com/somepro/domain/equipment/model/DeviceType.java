package com.somepro.domain.equipment.model;

import com.somepro.common.exception.BizException;

/**
 * 设备类别：ELEVATOR 电梯 / BOILER 锅炉 / PRESSURE_VESSEL 压力容器 / CRANE 起重机械。
 */
public enum DeviceType {

    ELEVATOR,
    BOILER,
    PRESSURE_VESSEL,
    CRANE;

    public static DeviceType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DeviceType.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException("设备类别只能是 ELEVATOR / BOILER / PRESSURE_VESSEL / CRANE：" + code);
        }
    }
}
