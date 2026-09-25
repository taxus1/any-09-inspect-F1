package com.somepro.interfaces.rest.equipment.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 除设备档案字段外带 unitName（仓储查询时回填的归属单位名称），列表页直接展示。
 * 刻意不含 delFlag / createBy / updateBy / updateTime 等内部字段。
 * deviceType / status 直接输出枚举名。
 */
public record EquipmentVO(Long id,
                          String regCode,
                          String deviceType,
                          Long unitId,
                          String unitName,
                          String installAddr,
                          LocalDate commissionDate,
                          Integer periodMonth,
                          LocalDate nextInspectDate,
                          String status,
                          LocalDateTime createTime) implements Serializable {
}
