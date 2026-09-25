package com.somepro.application.equipment;

import com.somepro.common.exception.BizException;
import com.somepro.domain.equipment.model.DeviceType;
import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.equipment.model.EquipmentStatus;
import com.somepro.domain.equipment.repository.EquipmentRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.useunit.model.UnitStatus;
import com.somepro.domain.useunit.model.UseUnit;
import com.somepro.domain.useunit.repository.UseUnitRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 设备应用服务：编排设备登记 / 改档 / 分页查询 / 删除等用例。
 *
 * 出入参都是领域对象（Equipment），不认识 PO、也不认识 VO。
 * 注册代码查重 + 归属单位合法性校验在这里做（库表唯一索引兜底）。
 */
@Service
public class EquipmentAppService {

    private final EquipmentRepository equipmentRepository;
    private final UseUnitRepository useUnitRepository;

    public EquipmentAppService(EquipmentRepository equipmentRepository,
                               UseUnitRepository useUnitRepository) {
        this.equipmentRepository = equipmentRepository;
        this.useUnitRepository = useUnitRepository;
    }

    /**
     * 登记设备。注册代码（如 SB-2026-0031）全局唯一；必须挂在一个在册单位名下。
     */
    public Mono<Equipment> register(String regCode, String deviceType, Long unitId, String installAddr,
                                    LocalDate commissionDate, Integer periodMonth, LocalDate nextInspectDate,
                                    String status) {
        DeviceType type = DeviceType.fromCode(deviceType);
        EquipmentStatus equipmentStatus = (status == null || status.isBlank())
                ? EquipmentStatus.IN_USE : EquipmentStatus.fromCode(status);
        int period = periodMonth == null ? 12 : periodMonth;
        return assertUnitAttachable(unitId)
                .then(assertRegCodeAvailable(regCode, null))
                .then(Mono.fromSupplier(() -> Equipment.create(regCode, type, unitId, installAddr,
                        commissionDate, period, nextInspectDate, equipmentStatus)))
                .flatMap(equipmentRepository::save);
    }

    /** 修改设备档案，支持注册代码纠错与改挂单位。 */
    public Mono<Equipment> update(Long id, String regCode, String deviceType, Long unitId, String installAddr,
                                  LocalDate commissionDate, Integer periodMonth, LocalDate nextInspectDate,
                                  String status) {
        return equipmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("设备不存在或已删除")))
                .flatMap(existing -> assertUnitAttachable(unitId)
                        .then(assertRegCodeAvailable(regCode, id))
                        .then(Mono.fromRunnable(() -> existing.update(regCode, DeviceType.fromCode(deviceType),
                                unitId, installAddr, commissionDate,
                                periodMonth == null ? 12 : periodMonth, nextInspectDate,
                                EquipmentStatus.fromCode(status))))
                        .then(equipmentRepository.save(existing)));
    }

    /** 分页查询：注册代码可模糊匹配，可按归属单位 / 类别 / 状态过滤；都不填即全量翻页。 */
    public Mono<PageResult<Equipment>> page(int pageNum, int pageSize, String regCode, Long unitId,
                                            String deviceType, String status) {
        DeviceType type = (deviceType == null || deviceType.isBlank()) ? null : DeviceType.fromCode(deviceType);
        EquipmentStatus equipmentStatus = (status == null || status.isBlank()) ? null : EquipmentStatus.fromCode(status);
        return equipmentRepository.page(pageNum, pageSize, regCode, unitId, type, equipmentStatus);
    }

    public Mono<Equipment> detail(Long id) {
        return equipmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("设备不存在或已删除")));
    }

    /** 删除（逻辑删除）设备档案。 */
    public Mono<Void> delete(Long id) {
        return equipmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("设备不存在或已删除")))
                .then(equipmentRepository.softDelete(id));
    }

    /** 归属单位必须存在且在册（ACTIVE）；已注销单位名下不能再挂设备。 */
    private Mono<UseUnit> assertUnitAttachable(Long unitId) {
        if (unitId == null) {
            return Mono.error(new BizException("设备必须归属一个使用单位"));
        }
        return useUnitRepository.findById(unitId)
                .switchIfEmpty(Mono.error(new BizException("归属单位不存在或已删除")))
                .flatMap(unit -> unit.getStatus() == UnitStatus.CANCELLED
                        ? Mono.error(new BizException("归属单位已注销，不能登记/挂载设备"))
                        : Mono.just(unit));
    }

    /** 注册代码查重：excludeId 用于改档时排除自身。 */
    private Mono<Void> assertRegCodeAvailable(String regCode, Long excludeId) {
        if (regCode == null || regCode.isBlank()) {
            return Mono.empty();
        }
        return equipmentRepository.findByRegCode(regCode.trim())
                .handle((found, sink) -> {
                    if (excludeId == null || !found.getId().equals(excludeId)) {
                        sink.error(new BizException("设备注册代码 " + regCode + " 已存在，编号不能重复"));
                    }
                });
    }
}
