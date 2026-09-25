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
 * t_equipment 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」，不放业务规则（规则在领域对象 Equipment）。
 * device_type / status 列存枚举 name() 字符串，不依赖 MyBatis 的枚举处理器。
 *
 * unitName 不是表列：查询时由仓储适配器按 unit_id 批量回填，标记 exist=false。
 *
 * 可空业务列统一 {@code updateStrategy = ALWAYS}：改档时允许把可空字段清空
 * （默认 NOT_NULL 策略会跳过 null，清空不会落库）。
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

    /** 归属单位名称：非表列，查询时回填。 */
    @TableField(exist = false)
    private String unitName;
}
