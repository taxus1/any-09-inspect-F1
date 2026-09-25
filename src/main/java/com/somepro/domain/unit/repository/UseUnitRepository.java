package com.somepro.domain.unit.repository;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.model.UnitStatus;
import com.somepro.domain.unit.model.UseUnit;
import reactor.core.publisher.Mono;

/**
 * 使用单位聚合的仓储端口（领域层定义，基础设施层实现，端口-适配器）。
 *
 * 分页返回领域自有 {@link PageResult}，不引入 Spring Data 的 Page。
 */
public interface UseUnitRepository {

    Mono<UseUnit> save(UseUnit unit);

    Mono<UseUnit> findById(Long id);

    /** 按单位编号查（编号全局唯一，仅查未逻辑删除的行）。 */
    Mono<UseUnit> findByCode(String unitCode);

    /**
     * 分页查单位。
     *
     * @param unitCode 编号关键字（模糊），可空
     * @param unitName 名称关键字（模糊），可空
     * @param status   状态精确过滤，可空
     * 两者都不填即翻全表，配合 pageNum/pageSize 一页页往后翻。
     */
    Mono<PageResult<UseUnit>> page(int pageNum, int pageSize, String unitCode, String unitName,
                                   UnitStatus status);

    /** 单位名下未删除设备数量（删单位前用，有家底的不能删）。 */
    Mono<Long> countEquipment(Long unitId);

    Mono<Void> softDelete(Long id);
}
