package com.somepro.domain.equipment.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 设备聚合根（特种设备档案：电梯 / 锅炉 / 压力容器 / 起重机械）。
 *
 * 一台设备挂在一个使用单位名下（unitId 指向 t_use_unit.id，归属关系在应用层校验）。
 * 纯领域对象：不带任何持久化注解（表映射在基础设施层的 EquipmentPO）。
 * ID 由仓储适配器用雪花算法分配（IdType.INPUT）。
 *
 * 不变量：
 * - 注册代码（如 SB-2026-0031）必填、去空格；全局唯一性由应用层校验 + 库表 uk_reg_code 兜底。
 * - 类别只能是 {@link DeviceType} 之一。
 * - 必须归属一个单位（unitId 必填）。
 * - 检验周期为正整数（月），默认 12。
 * - 下次检验到期日不能早于投用日期（都填时校验）。
 *
 * unitName 不属于本表：由仓储查询时按 unitId 批量回填，供接口层展示，不落 t_equipment。
 */
@Getter
@Setter
public class Equipment extends BaseEntity {

    private Long id;

    /** 设备注册代码，如 SB-2026-0031（全局唯一）。 */
    private String regCode;

    private DeviceType deviceType;

    /** 归属使用单位 id。 */
    private Long unitId;

    /** 归属单位名称（查询回填，非持久化字段）。 */
    private String unitName;

    /** 安装地点。 */
    private String installAddr;

    /** 投用日期。 */
    private LocalDate commissionDate;

    /** 法定检验周期（月）。 */
    private Integer periodMonth;

    /** 下次检验到期日。 */
    private LocalDate nextInspectDate;

    private EquipmentStatus status;

    /** 工厂方法：登记新设备，保证初始不变量。 */
    public static Equipment create(String regCode, DeviceType deviceType, Long unitId, String installAddr,
                                   LocalDate commissionDate, Integer periodMonth, LocalDate nextInspectDate,
                                   EquipmentStatus status) {
        Equipment equipment = new Equipment();
        equipment.update(regCode, deviceType, unitId, installAddr, commissionDate, periodMonth,
                nextInspectDate, status == null ? EquipmentStatus.IN_USE : status);
        return equipment;
    }

    /** 领域行为：修改设备档案（含注册代码纠错）。 */
    public void update(String regCode, DeviceType deviceType, Long unitId, String installAddr,
                       LocalDate commissionDate, Integer periodMonth, LocalDate nextInspectDate,
                       EquipmentStatus status) {
        if (regCode == null || regCode.isBlank()) {
            throw new BizException("设备注册代码不能为空");
        }
        if (deviceType == null) {
            throw new BizException("设备类别不能为空");
        }
        if (unitId == null) {
            throw new BizException("设备必须归属一个使用单位");
        }
        if (periodMonth == null || periodMonth <= 0) {
            throw new BizException("检验周期（月）必须为正整数");
        }
        if (status == null) {
            throw new BizException("设备状态不能为空：IN_USE / SUSPENDED / SEALED / SCRAPPED");
        }
        if (commissionDate != null && nextInspectDate != null
                && nextInspectDate.isBefore(commissionDate)) {
            throw new BizException("下次检验到期日不能早于投用日期");
        }
        this.regCode = regCode.trim();
        this.deviceType = deviceType;
        this.unitId = unitId;
        this.installAddr = trimToNull(installAddr);
        this.commissionDate = commissionDate;
        this.periodMonth = periodMonth;
        this.nextInspectDate = nextInspectDate;
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
