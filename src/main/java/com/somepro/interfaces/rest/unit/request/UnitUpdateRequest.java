package com.somepro.interfaces.rest.unit.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 修改使用单位档案请求（含编号——填错了支持改）。
 */
public record UnitUpdateRequest(
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

        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "ACTIVE|CANCELLED", message = "状态只能是 ACTIVE 或 CANCELLED")
        String status) implements Serializable {
}
