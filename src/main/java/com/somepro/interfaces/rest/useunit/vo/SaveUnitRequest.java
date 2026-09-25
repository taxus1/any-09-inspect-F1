package com.somepro.interfaces.rest.useunit.vo;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 使用单位登记/改档请求体（用户接口层）—— 不可变 record。
 *
 * 登记和改档共用：必填校验交给 Bean Validation（WebFlux 下失败抛 WebExchangeBindException，
 * 已由 GlobalExceptionHandler 收口成中文提示）；枚举合法性与编号唯一性在应用/领域层校验。
 *
 * 注意：status 不在此做枚举硬校验 —— 非法值要回「只允许 ACTIVE / CANCELLED」而不是
 * Bean Validation 的模板英文文案。
 */
public record SaveUnitRequest(@NotBlank(message = "单位编号不能为空") String unitCode,
                              @NotBlank(message = "单位名称不能为空") String unitName,
                              String creditCode,
                              String district,
                              String contactName,
                              String contactPhone,
                              String status) implements Serializable {
}
