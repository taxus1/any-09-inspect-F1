package com.somepro.domain.useunit.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用单位聚合根（特种设备使用单位档案）。
 *
 * 纯领域对象：只描述业务与不变量，不带任何持久化注解（表映射在基础设施层的 UseUnitPO）。
 * ID 由仓储适配器用雪花算法分配（IdType.INPUT）。
 *
 * 不变量：
 * - 单位编号（如 SY-0031）必填、去空格；全局唯一性由应用层校验 + 库表 uk_unit_code 兜底。
 * - 单位名称必填。
 * - 状态只能是 {@link UnitStatus} 之一，新建默认 ACTIVE。
 */
@Getter
@Setter
public class UseUnit extends BaseEntity {

    private Long id;

    /** 单位编号，如 SY-0031（全局唯一）。 */
    private String unitCode;

    /** 单位名称。 */
    private String unitName;

    /** 统一社会信用代码。 */
    private String creditCode;

    /** 所在区县。 */
    private String district;

    /** 安全管理员姓名。 */
    private String contactName;

    /** 安全管理员联系电话。 */
    private String contactPhone;

    private UnitStatus status;

    /** 工厂方法：登记新单位，保证初始不变量。 */
    public static UseUnit create(String unitCode, String unitName, String creditCode, String district,
                                 String contactName, String contactPhone, UnitStatus status) {
        UseUnit unit = new UseUnit();
        unit.update(unitCode, unitName, creditCode, district, contactName, contactPhone,
                status == null ? UnitStatus.ACTIVE : status);
        return unit;
    }

    /** 领域行为：修改单位档案（含编号纠错）。状态不允许置空。 */
    public void update(String unitCode, String unitName, String creditCode, String district,
                       String contactName, String contactPhone, UnitStatus status) {
        if (unitCode == null || unitCode.isBlank()) {
            throw new BizException("单位编号不能为空");
        }
        if (unitName == null || unitName.isBlank()) {
            throw new BizException("单位名称不能为空");
        }
        if (status == null) {
            throw new BizException("单位状态不能为空：ACTIVE 在册 / CANCELLED 注销");
        }
        this.unitCode = unitCode.trim();
        this.unitName = unitName.trim();
        this.creditCode = trimToNull(creditCode);
        this.district = trimToNull(district);
        this.contactName = trimToNull(contactName);
        this.contactPhone = trimToNull(contactPhone);
        this.status = status;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
