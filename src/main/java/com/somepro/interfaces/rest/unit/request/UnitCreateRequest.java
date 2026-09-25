package com.somepro.interfaces.rest.unit.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 登记使用单位请求。状态可不传（领域默认 ACTIVE），传则只能是 ACTIVE / CANCELLED。
 */
public record UnitCreateRequest(
        @NotBlank(message = "单位编号不能为空")
        @Size(max = 32, message = "单位编号最长 32 位")
        String unitCode,

        @NotBlank(message = "单位名称不能为空")
        @Size(max = 128, message = "单位名称最长 128 位")
        String unitName,

        @Size(max = 32, message = "统一社会信用代码最长 32 位")
        String creditCode,

        @Size(max = 64, message = "区县最长 64 位")
        String district,

        @Size(max = 64, message = "安全管理员姓名最长 64 位")
        String contactName,

        @Size(max = 32, message = "联系电话最长 32 位")
        String contactPhone,

        @Pattern(regexp = "ACTIVE|CANCELLED", message = "状态只能是 ACTIVE 或 CANCELLED")
        String status) implements Serializable {
}
