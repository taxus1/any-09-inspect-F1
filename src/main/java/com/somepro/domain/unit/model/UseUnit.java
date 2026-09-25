package com.somepro.domain.unit.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用单位聚合根（领域层）：一家使用单位及其档案信息。
 *
 * 纯领域对象，不带任何持久化注解；编号全局唯一这一不变量由应用层用仓储端口校验，
 * DB 上还有 uk_unit_code 兜底。一台设备挂在且仅挂在一个单位名下（见 Equipment）。
 */
@Getter
@Setter
public class UseUnit extends BaseEntity {

    private Long id;

    /** 单位编号，全局唯一，如 SY-0031。 */
    private String unitCode;

    /** 单位名称。 */
    private String unitName;

    /** 统一社会信用代码，可空。 */
    private String creditCode;

    /** 所在区县。 */
    private String district;

    /** 安全管理员姓名。 */
    private String contactName;

    /** 安全管理员联系电话。 */
    private String contactPhone;

    /** 在册 ACTIVE / 注销 CANCELLED，新登记默认在册。 */
    private UnitStatus status;

    /** 工厂方法：登记新单位并保证初始不变量。 */
    public static UseUnit register(String unitCode, String unitName, String creditCode,
                                   String district, String contactName, String contactPhone,
                                   UnitStatus status) {
        UseUnit unit = new UseUnit();
        unit.modify(unitCode, unitName, creditCode, district, contactName, contactPhone, status);
        return unit;
    }

    /**
     * 领域行为：修改可编辑档案（含编号——填错了允许改，唯一性由应用层校验）。
     * 状态没传时保持/落为 ACTIVE。
     */
    public void modify(String unitCode, String unitName, String creditCode,
                       String district, String contactName, String contactPhone,
                       UnitStatus status) {
        if (unitCode == null || unitCode.isBlank()) {
            throw new BizException("单位编号不能为空");
        }
        if (unitName == null || unitName.isBlank()) {
            throw new BizException("单位名称不能为空");
        }
        this.unitCode = unitCode.trim();
        this.unitName = unitName.trim();
        this.creditCode = normalize(creditCode);
        this.district = normalize(district);
        this.contactName = normalize(contactName);
        this.contactPhone = normalize(contactPhone);
        this.status = status == null ? UnitStatus.ACTIVE : status;
    }

    /** 空白串统一归一为 null，可空字段落库即 NULL 而不是 ""。 */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
