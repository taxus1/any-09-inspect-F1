package com.somepro.interfaces.rest.useunit.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 使用单位对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 只暴露允许外部看到的档案字段；刻意不含 delFlag / createBy / updateBy / updateTime 等内部字段。
 * status 直接输出枚举名（ACTIVE / CANCELLED）。
 */
public record UseUnitVO(Long id,
                        String unitCode,
                        String unitName,
                        String creditCode,
                        String district,
                        String contactName,
                        String contactPhone,
                        String status,
                        LocalDateTime createTime) implements Serializable {
}
