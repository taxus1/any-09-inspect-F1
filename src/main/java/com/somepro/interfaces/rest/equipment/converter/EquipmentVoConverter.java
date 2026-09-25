package com.somepro.interfaces.rest.equipment.converter;

import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.equipment.vo.EquipmentVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Equipment（领域）→ EquipmentVO（对外）转换器（用户接口层）。
 */
public final class EquipmentVoConverter {

    private EquipmentVoConverter() {
    }

    public static EquipmentVO toVo(Equipment domain) {
        return new EquipmentVO(
                domain.getId(),
                domain.getRegCode(),
                domain.getDeviceType() == null ? null : domain.getDeviceType().name(),
                domain.getUnitId(),
                domain.getUnitCode(),
                domain.getUnitName(),
                domain.getInstallAddr(),
                domain.getCommissionDate(),
                domain.getPeriodMonth(),
                domain.getNextInspectDate(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static PageVO<EquipmentVO> toPageVo(PageResult<Equipment> page) {
        List<EquipmentVO> content = page.content().stream()
                .map(EquipmentVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
