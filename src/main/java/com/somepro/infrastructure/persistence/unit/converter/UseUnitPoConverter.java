package com.somepro.infrastructure.persistence.unit.converter;

import com.somepro.domain.unit.model.UnitStatus;
import com.somepro.domain.unit.model.UseUnit;
import com.somepro.infrastructure.persistence.unit.po.UseUnitPO;

/**
 * UseUnitPO（表）↔ UseUnit（领域）唯一转换入口（基础设施层）。
 * status 在表里是 VARCHAR 英文码，领域里是枚举。
 */
public final class UseUnitPoConverter {

    private UseUnitPoConverter() {
    }

    public static UseUnitPO toPo(UseUnit domain) {
        UseUnitPO po = new UseUnitPO();
        po.setId(domain.getId());
        po.setUnitCode(domain.getUnitCode());
        po.setUnitName(domain.getUnitName());
        po.setCreditCode(domain.getCreditCode());
        po.setDistrict(domain.getDistrict());
        po.setContactName(domain.getContactName());
        po.setContactPhone(domain.getContactPhone());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static UseUnit toDomain(UseUnitPO po) {
        UseUnit domain = new UseUnit();
        domain.setId(po.getId());
        domain.setUnitCode(po.getUnitCode());
        domain.setUnitName(po.getUnitName());
        domain.setCreditCode(po.getCreditCode());
        domain.setDistrict(po.getDistrict());
        domain.setContactName(po.getContactName());
        domain.setContactPhone(po.getContactPhone());
        domain.setStatus(po.getStatus() == null ? null : UnitStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
