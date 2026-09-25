package com.somepro.domain.equipment.repository;

import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 设备档案聚合的仓储端口（领域层定义，基础设施层实现）。
 */
public interface EquipmentRepository {

    Mono<Equipment> save(Equipment equipment);

    Mono<Equipment> findById(Long id);

    /** 按注册代码查（全局唯一，仅查未逻辑删除的行）。 */
    Mono<Equipment> findByRegCode(String regCode);

    /** 统计某单位名下未删除设备数。 */
    Mono<Long> countByUnit(Long unitId);

    /**
     * 分页查设备。
     *
     * @param regCode 注册代码关键字（模糊），可空
     * @param unitName 归属单位名称关键字（模糊，先查单位 id 再过滤），可空
     * @param unitId  归属单位精确过滤，可空
     * 都不填即翻全表。返回结果由仓储回填单位编号/名称快照。
     */
    Mono<PageResult<Equipment>> page(int pageNum, int pageSize, String regCode,
                                     String unitName, Long unitId);
}
