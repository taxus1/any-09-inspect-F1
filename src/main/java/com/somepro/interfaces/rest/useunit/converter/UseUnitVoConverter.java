package com.somepro.interfaces.rest.useunit.converter;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.useunit.model.UseUnit;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.useunit.vo.UseUnitVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UseUnit（领域）→ UseUnitVO（对外）转换器（用户接口层）。
 *
 * Controller 不许直接把领域对象塞进 Result 返回，否则 delFlag / createBy / updateBy
 * 等内部字段会被无意识序列化出去。
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
