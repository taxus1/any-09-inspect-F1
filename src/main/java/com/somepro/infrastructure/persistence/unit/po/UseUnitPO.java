package com.somepro.infrastructure.persistence.unit.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_use_unit 表的持久化对象（基础设施层）。字段与列一一对应，不放业务规则。
 *
 * 可空列统一 {@code updateStrategy = ALWAYS}：登记后支持改成「清空」，
 * 否则 MyBatis-Plus 默认 NOT_NULL 策略会跳过 null 字段，老值清不掉。
 * 编号虽在 DB 上有唯一索引，唯一性校验仍在应用层先做、DB 兜底。
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
