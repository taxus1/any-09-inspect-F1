package com.somepro.infrastructure.persistence.equipment.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * t_equipment 表的持久化对象（基础设施层）。字段与列一一对应，不放业务规则。
 *
 * 可空列统一 {@code updateStrategy = ALWAYS}：改档案时允许把安装地点、日期等清空。
 * device_type / status 存枚举英文码。
 */
@Getter
@Setter
@TableName("t_equipment")
public class EquipmentPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("reg_code")
    private String regCode;

    @TableField("device_type")
    private String deviceType;

    @TableField("unit_id")
    private Long unitId;

    @TableField(value = "install_addr", updateStrategy = FieldStrategy.ALWAYS)
    private String installAddr;

    @TableField(value = "commission_date", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate commissionDate;

    @TableField("period_month")
    private Integer periodMonth;

    @TableField(value = "next_inspect_date", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate nextInspectDate;

    @TableField("status")
    private String status;
}
