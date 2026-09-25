package com.somepro.domain.useunit.repository;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.useunit.model.UseUnit;
import reactor.core.publisher.Mono;

/**
 * 使用单位聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 编号唯一性：{@link #findByCode} 供应用层登记/改号前查重；库表 uk_unit_code 是最后一道防线。
 */
public interface UseUnitRepository {

    Mono<UseUnit> save(UseUnit unit);

    Mono<UseUnit> findById(Long id);

    /** 按单位编号查未删除单位；查不到回空信号。 */
    Mono<UseUnit> findByCode(String unitCode);

    /**
     * 分页查单位。
     *
     * @param unitName 单位名称模糊匹配，可空（不填不过滤）
     * @param unitCode 单位编号模糊匹配，可空（不填不过滤）
     */
    Mono<PageResult<UseUnit>> page(int pageNum, int pageSize, String unitName, String unitCode);

    Mono<Void> softDelete(Long id);
}
