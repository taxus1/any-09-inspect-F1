package com.somepro.application.useunit;

import com.somepro.common.exception.BizException;
import com.somepro.domain.equipment.repository.EquipmentRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.useunit.model.UnitStatus;
import com.somepro.domain.useunit.model.UseUnit;
import com.somepro.domain.useunit.repository.UseUnitRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 使用单位应用服务：编排单位登记 / 改档 / 注销状态 / 分页查询 / 删除等用例。
 *
 * 出入参都是领域对象（UseUnit），不认识 PO、也不认识 VO。
 * 编号查重在这里做（库表唯一索引兜底，并发冲突由全局异常处理转成中文提示）。
 */
@Service
public class UseUnitAppService {

    private final UseUnitRepository useUnitRepository;
    private final EquipmentRepository equipmentRepository;

    public UseUnitAppService(UseUnitRepository useUnitRepository,
                             EquipmentRepository equipmentRepository) {
        this.useUnitRepository = useUnitRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /** 登记新单位。编号（如 SY-0031）全局唯一。 */
    public Mono<UseUnit> register(String unitCode, String unitName, String creditCode, String district,
                                  String contactName, String contactPhone, String status) {
        UnitStatus unitStatus = status == null || status.isBlank() ? UnitStatus.ACTIVE : UnitStatus.fromCode(status);
        UseUnit unit = UseUnit.create(unitCode, unitName, creditCode, district,
                contactName, contactPhone, unitStatus);
        return assertCodeAvailable(unit.getUnitCode(), null)
                .then(useUnitRepository.save(unit));
    }

    /**
     * 修改单位档案，支持编号纠错（把填错的 SY-0031 改成正确编号）。
     * 改后的编号不能和别的单位撞。
     */
    public Mono<UseUnit> update(Long id, String unitCode, String unitName, String creditCode, String district,
                                String contactName, String contactPhone, String status) {
        return useUnitRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("单位不存在或已删除")))
                .flatMap(existing -> assertCodeAvailable(unitCode == null ? null : unitCode.trim(), id)
                        .then(Mono.fromRunnable(() -> existing.update(unitCode, unitName, creditCode, district,
                                contactName, contactPhone, UnitStatus.fromCode(status))))
                        .then(useUnitRepository.save(existing)));
    }

    /** 分页查询：单位名称、单位编号都可模糊匹配；两个都不填即全量翻页。 */
    public Mono<PageResult<UseUnit>> page(int pageNum, int pageSize, String unitName, String unitCode) {
        return useUnitRepository.page(pageNum, pageSize, unitName, unitCode);
    }

    public Mono<UseUnit> detail(Long id) {
        return useUnitRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("单位不存在或已删除")));
    }

    /**
     * 删除（逻辑删除）不再使用的单位。
     * 名下还挂着设备的单位不能删 —— 否则设备会变成找不到归属单位的孤儿数据。
     */
    public Mono<Void> delete(Long id) {
        return useUnitRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("单位不存在或已删除")))
                .flatMap(unit -> equipmentRepository.countByUnitId(id)
                        .flatMap(count -> count > 0
                                ? Mono.error(new BizException("该单位名下还有 " + count + " 台设备，不能删除"))
                                : useUnitRepository.softDelete(id)));
    }

    /** 编号查重：excludeId 用于改档时排除自身。 */
    private Mono<Void> assertCodeAvailable(String unitCode, Long excludeId) {
        if (unitCode == null || unitCode.isBlank()) {
            return Mono.empty();
        }
        return useUnitRepository.findByCode(unitCode.trim())
                .handle((found, sink) -> {
                    if (excludeId == null || !found.getId().equals(excludeId)) {
                        sink.error(new BizException("单位编号 " + unitCode + " 已存在，编号不能重复"));
                    }
                });
    }
}
