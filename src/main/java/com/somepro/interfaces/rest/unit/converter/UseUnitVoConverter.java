package com.somepro.interfaces.rest.unit.converter;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.model.UseUnit;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.unit.vo.UseUnitVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UseUnit（领域）→ UseUnitVO（对外）转换器（用户接口层），Controller 不直接返回领域对象。
 */
public final class UseUnitVoConverter {

    private UseUnitVoConverter() {
    }

    public static UseUnitVO toVo(UseUnit domain) {
        return new UseUnitVO(
                domain.getId(),
                domain.getUnitCode(),
                domain.getUnitName(),
                domain.getCreditCode(),
                domain.getDistrict(),
                domain.getContactName(),
                domain.getContactPhone(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static PageVO<UseUnitVO> toPageVo(PageResult<UseUnit> page) {
        List<UseUnitVO> content = page.content().stream()
                .map(UseUnitVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
