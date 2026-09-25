package com.somepro.application.equipment;

import com.somepro.common.exception.BizException;
import com.somepro.domain.equipment.model.DeviceType;
import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.equipment.model.EquipmentStatus;
import com.somepro.domain.equipment.repository.EquipmentRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.repository.UseUnitRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 设备档案应用服务：编排登记 / 修改 / 分页查询用例。
 *
 * 一台设备必须挂在一个单位名下，所以写入前先校验单位存在且未删除。
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

    public Mono<Equipment> register(String regCode, String typeCode, Long unitId,
                                    String installAddr, LocalDate commissionDate,
                                    Integer periodMonth, LocalDate nextInspectDate,
                                    String statusCode) {
        String code = regCode.trim();
        // 先确认归属单位存在，再查重注册代码，最后构造并落库
        return requireUnit(unitId)
                .then(ensureRegCodeFree(code))
                .then(Mono.defer(() -> {
                    Equipment equipment = Equipment.register(code, DeviceType.fromCode(typeCode), unitId,
                            installAddr, commissionDate, periodMonth, nextInspectDate,
                            EquipmentStatus.fromCode(statusCode));
                    return saveWithUniqueGuard(equipment, "设备注册代码已存在：" + code);
                }));
    }

    public Mono<Equipment> modify(Long id, String regCode, String typeCode, Long unitId,
                                  String installAddr, LocalDate commissionDate,
                                  Integer periodMonth, LocalDate nextInspectDate,
                                  String statusCode) {
        String code = regCode.trim();
        return requireEquipment(id)
                .flatMap(equipment -> requireUnit(unitId).thenReturn(equipment))
                .flatMap(equipment -> ensureRegCodeFree(code, id).thenReturn(equipment))
                .flatMap(equipment -> {
                    equipment.modify(code, DeviceType.fromCode(typeCode), unitId, installAddr,
                            commissionDate, periodMonth, nextInspectDate,
                            EquipmentStatus.fromCode(statusCode));
                    return saveWithUniqueGuard(equipment, "设备注册代码已被占用：" + code);
                });
    }

    public Mono<Equipment> detail(Long id) {
        return requireEquipment(id);
    }

    public Mono<PageResult<Equipment>> page(int pageNum, int pageSize,
                                            String regCode, String unitName, Long unitId) {
        return equipmentRepository.page(pageNum, pageSize, regCode, unitName, unitId);
    }

    private Mono<Void> requireUnit(Long unitId) {
        if (unitId == null) {
            // 领域层还会再兜一次，这里提前报能少一次 DB 往返
            return Mono.error(new BizException("设备必须挂在一个使用单位名下"));
        }
        return useUnitRepository.findById(unitId)
                .switchIfEmpty(Mono.error(new BizException("归属单位不存在或已删除：id=" + unitId)))
                .then();
    }

    private Mono<Equipment> requireEquipment(Long id) {
        return equipmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("设备不存在或已删除：id=" + id)));
    }

    /** 注册代码必须没人占用。 */
    private Mono<Void> ensureRegCodeFree(String code) {
        return ensureRegCodeFree(code, null);
    }

    /** 修改时允许「占用者就是自己」。 */
    private Mono<Void> ensureRegCodeFree(String code, Long selfId) {
        return equipmentRepository.findByRegCode(code)
                .flatMap(existing -> {
                    if (selfId != null && existing.getId().equals(selfId)) {
                        return Mono.<Void>empty();
                    }
                    return Mono.error(new BizException("设备注册代码已存在：" + code));
                });
    }

    /** uk_reg_code 的并发兜底。 */
    private Mono<Equipment> saveWithUniqueGuard(Equipment equipment, String message) {
        return equipmentRepository.save(equipment)
                .onErrorResume(DuplicateKeyException.class,
                        e -> Mono.error(new BizException(message)));
    }
}
