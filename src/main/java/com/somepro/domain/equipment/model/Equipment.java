package com.somepro.domain.equipment.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 设备档案聚合根（领域层）：一台特种设备挂在且仅挂在一个使用单位名下。
 *
 * 纯领域对象，不带任何持久化注解；注册代码全局唯一这一不变量由应用层用仓储端口校验，
 * DB 上还有 uk_reg_code 兜底。
 *
 * {@code unitCode / unitName} 不落 t_equipment，是仓储读取时按 {@link #unitId} 回填的
 * 归属单位快照（列表要展示「设备属于哪家单位」），由仓储负责、不由调用方填写。
 */
@Getter
@Setter
public class Equipment extends BaseEntity {

    private Long id;

    /** 设备注册代码，全局唯一，如 SB-2026-0031。 */
    private String regCode;

    /** 设备类别。 */
    private DeviceType deviceType;

    /** 归属使用单位 id。 */
    private Long unitId;

    /** 安装地点。 */
    private String installAddr;

    /** 投用日期。 */
    private LocalDate commissionDate;

    /** 法定检验周期（月）。 */
    private Integer periodMonth;

    /** 下次检验到期日。 */
    private LocalDate nextInspectDate;

    /** 在用 / 停用 / 封存 / 已报废，新登记默认在用。 */
    private EquipmentStatus status;

    // ---- 以下为仓储回填的归属单位快照，非持久化字段 ----

    /** 归属单位编号（仓储回填）。 */
    private String unitCode;

    /** 归属单位名称（仓储回填）。 */
    private String unitName;

    /** 工厂方法：登记新设备并保证初始不变量。 */
    public static Equipment register(String regCode, DeviceType deviceType, Long unitId,
                                     String installAddr, LocalDate commissionDate,
                                     Integer periodMonth, LocalDate nextInspectDate,
                                     EquipmentStatus status) {
        Equipment equipment = new Equipment();
        equipment.modify(regCode, deviceType, unitId, installAddr, commissionDate,
                periodMonth, nextInspectDate, status);
        return equipment;
    }

    /** 领域行为：修改可编辑档案（含注册代码——填错了允许改，唯一性由应用层校验）。 */
    public void modify(String regCode, DeviceType deviceType, Long unitId,
                       String installAddr, LocalDate commissionDate,
                       Integer periodMonth, LocalDate nextInspectDate,
                       EquipmentStatus status) {
        if (regCode == null || regCode.isBlank()) {
            throw new BizException("设备注册代码不能为空");
        }
        if (deviceType == null) {
            throw new BizException("设备类别不能为空");
        }
        if (unitId == null) {
            throw new BizException("设备必须挂在一个使用单位名下");
        }
        if (periodMonth == null || periodMonth <= 0) {
            throw new BizException("检验周期必须是正整数（月）");
        }
        this.regCode = regCode.trim();
        this.deviceType = deviceType;
        this.unitId = unitId;
        this.installAddr = normalize(installAddr);
        this.commissionDate = commissionDate;
        this.periodMonth = periodMonth;
        this.nextInspectDate = nextInspectDate;
        this.status = status == null ? EquipmentStatus.IN_USE : status;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
