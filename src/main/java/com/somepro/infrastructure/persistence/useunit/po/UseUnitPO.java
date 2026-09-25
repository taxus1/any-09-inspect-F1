package com.somepro.infrastructure.persistence.useunit.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_use_unit 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」，不放业务规则（规则在领域对象 UseUnit）。
 * status 列存枚举 name() 字符串，不依赖 MyBatis 的枚举处理器（转换在 UseUnitPoConverter）。
 *
 * 可空业务列统一 {@code updateStrategy = ALWAYS}：改档时允许把可空字段清空
 * （默认 NOT_NULL 策略会跳过 null，清空不会落库）。必填列不受影响。
 */
@Getter
@Setter
@TableName("t_use_unit")
public class UseUnitPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("unit_code")
    private String unitCode;

    @TableField("unit_name")
    private String unitName;

    @TableField(value = "credit_code", updateStrategy = FieldStrategy.ALWAYS)
    private String creditCode;

    @TableField(value = "district", updateStrategy = FieldStrategy.ALWAYS)
    private String district;

    @TableField(value = "contact_name", updateStrategy = FieldStrategy.ALWAYS)
    private String contactName;

    @TableField(value = "contact_phone", updateStrategy = FieldStrategy.ALWAYS)
    private String contactPhone;

    @TableField("status")
    private String status;
}
