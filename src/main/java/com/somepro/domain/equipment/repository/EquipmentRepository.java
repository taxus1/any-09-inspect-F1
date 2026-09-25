package com.somepro.domain.equipment.repository;

import com.somepro.domain.equipment.model.DeviceType;
import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.equipment.model.EquipmentStatus;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 设备聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 注册代码唯一性：{@link #findByRegCode} 供应用层登记/改号前查重；库表 uk_reg_code 是最后一道防线。
 */
public interface EquipmentRepository {

    Mono<Equipment> save(Equipment equipment);

    Mono<Equipment> findById(Long id);

    /** 按注册代码查未删除设备；查不到回空信号。 */
    Mono<Equipment> findByRegCode(String regCode);

    /**
     * 分页查设备（结果回填 unitName 供展示）。
     *
     * @param regCode  注册代码模糊匹配，可空
     * @param unitId   归属单位 id，可空
     * @param type     设备类别，可空
     * @param status   设备状态，可空
     */
    Mono<PageResult<Equipment>> page(int pageNum, int pageSize, String regCode, Long unitId,
                                     DeviceType type, EquipmentStatus status);

    /** 统计单位名下未删除的设备数量（删除单位前做归属校验）。 */
    Mono<Long> countByUnitId(Long unitId);

    Mono<Void> softDelete(Long id);
}
