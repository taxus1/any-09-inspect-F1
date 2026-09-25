package com.somepro.interfaces.rest.equipment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 修改设备档案请求（含注册代码——填错了支持改；也可改挂到别的单位）。
 */
public record EquipmentUpdateRequest(
        @NotBlank(message = "设备注册代码不能为空")
        @Size(max = 32, message = "注册代码最长 32 位")
        String regCode,

        @NotBlank(message = "设备类别不能为空")
        @Pattern(regexp = "ELEVATOR|BOILER|PRESSURE_VESSEL|CRANE",
                message = "设备类别只能是 ELEVATOR / BOILER / PRESSURE_VESSEL / CRANE")
        String deviceType,

        @NotNull(message = "必须指定归属单位 id")
        Long unitId,

        @Size(max = 255, message = "安装地点最长 255 位")
        String installAddr,

        LocalDate commissionDate,

        @NotNull(message = "检验周期不能为空")
        @Positive(message = "检验周期必须是正整数（月）")
        Integer periodMonth,

        LocalDate nextInspectDate,

        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "IN_USE|SUSPENDED|SEALED|SCRAPPED",
                message = "状态只能是 IN_USE / SUSPENDED / SEALED / SCRAPPED")
        String status) implements Serializable {
}
