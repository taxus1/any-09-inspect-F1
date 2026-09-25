package com.somepro.infrastructure.persistence.equipment.converter;

import com.somepro.domain.equipment.model.DeviceType;
import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.equipment.model.EquipmentStatus;
import com.somepro.infrastructure.persistence.equipment.po.EquipmentPO;

/**
 * EquipmentPO（表）↔ Equipment（领域）唯一转换入口（基础设施层）。
 * 设备类别 / 状态在表里是 VARCHAR 英文码，领域里是枚举。
 * unitCode / unitName 是仓储回填的归属单位快照，PO 里没有，这里不搬运。
 */
public final class EquipmentPoConverter {

    private EquipmentPoConverter() {
    }

    public static EquipmentPO toPo(Equipment domain) {
        EquipmentPO po = new EquipmentPO();
        po.setId(domain.getId());
        po.setRegCode(domain.getRegCode());
        po.setDeviceType(domain.getDeviceType() == null ? null : domain.getDeviceType().name());
        po.setUnitId(domain.getUnitId());
        po.setInstallAddr(domain.getInstallAddr());
        po.setCommissionDate(domain.getCommissionDate());
        po.setPeriodMonth(domain.getPeriodMonth());
        po.setNextInspectDate(domain.getNextInspectDate());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static Equipment toDomain(EquipmentPO po) {
        Equipment domain = new Equipment();
        domain.setId(po.getId());
        domain.setRegCode(po.getRegCode());
        domain.setDeviceType(po.getDeviceType() == null ? null : DeviceType.valueOf(po.getDeviceType()));
        domain.setUnitId(po.getUnitId());
        domain.setInstallAddr(po.getInstallAddr());
        domain.setCommissionDate(po.getCommissionDate());
        domain.setPeriodMonth(po.getPeriodMonth());
        domain.setNextInspectDate(po.getNextInspectDate());
        domain.setStatus(po.getStatus() == null ? null : EquipmentStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
