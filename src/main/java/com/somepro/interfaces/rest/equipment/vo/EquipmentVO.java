package com.somepro.interfaces.rest.equipment.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备档案对外返回对象（VO，用户接口层）—— 不可变 record。
 * unitCode / unitName 是仓储回填的归属单位快照，方便列表直接展示「设备属于哪家单位」。
 */
public record EquipmentVO(Long id,
                          String regCode,
                          String deviceType,
                          Long unitId,
                          String unitCode,
                          String unitName,
                          String installAddr,
                          LocalDate commissionDate,
                          Integer periodMonth,
                          LocalDate nextInspectDate,
                          String status,
                          LocalDateTime createTime) implements Serializable {
}
