package com.somepro.interfaces.rest.equipment.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 设备登记/改档请求体（用户接口层）—— 不可变 record。
 *
 * 登记和改档共用：必填校验交给 Bean Validation；枚举合法性、归属单位在册校验、
 * 注册代码唯一性、日期先后关系在应用/领域层校验（错误信息更贴合业务）。
 *
 * periodMonth 可空：不传时应用层按默认 12 个月处理。
 * status 不做枚举硬校验：非法值回「只允许 IN_USE / SUSPENDED / SEALED / SCRAPPED」。
 */
public record SaveEquipmentRequest(@NotBlank(message = "设备注册代码不能为空") String regCode,
                                   @NotBlank(message = "设备类别不能为空") String deviceType,
                                   @NotNull(message = "必须指定归属单位 id") Long unitId,
                                   String installAddr,
                                   LocalDate commissionDate,
                                   @Positive(message = "检验周期（月）必须为正整数") Integer periodMonth,
                                   LocalDate nextInspectDate,
                                   String status) implements Serializable {
}
