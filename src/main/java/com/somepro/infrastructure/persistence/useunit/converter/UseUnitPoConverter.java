package com.somepro.infrastructure.persistence.useunit.converter;

import com.somepro.domain.useunit.model.UnitStatus;
import com.somepro.domain.useunit.model.UseUnit;
import com.somepro.infrastructure.persistence.useunit.po.UseUnitPO;

/**
 * UseUnitPO（表）↔ UseUnit（领域）转换器（基础设施层）。
 *
 * PO 与领域模型之间唯一的转换入口：领域层和接口层都不应看到 UseUnitPO。
 * status 以枚举 name() 落库（ACTIVE / CANCELLED）。
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
