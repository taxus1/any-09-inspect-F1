package com.somepro.interfaces.rest.unit.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 使用单位对外返回对象（VO，用户接口层）—— 不可变 record。
 * 只暴露档案字段；delFlag 与审计人等内部字段不进 API 契约。
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
