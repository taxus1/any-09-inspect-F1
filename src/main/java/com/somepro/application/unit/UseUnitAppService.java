package com.somepro.application.unit;

import com.somepro.common.exception.BizException;
import com.somepro.domain.equipment.repository.EquipmentRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.model.UnitStatus;
import com.somepro.domain.unit.model.UseUnit;
import com.somepro.domain.unit.repository.UseUnitRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 使用单位应用服务：编排登记 / 修改 / 分页查询 / 删除用例，不写字段级规则（规则在领域聚合）。
 *
 * 入参出参都是领域对象，不认识 PO / VO。
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

    /** 登记新单位：编号全局唯一，DB 唯一索引兜底并发。 */
    public Mono<UseUnit> register(String unitCode, String unitName, String creditCode,
                                  String district, String contactName, String contactPhone,
                                  String statusCode) {
        String code = unitCode.trim();
        return useUnitRepository.findByCode(code)
                .flatMap(existing -> Mono.<UseUnit>error(new BizException("单位编号已存在：" + code)))
                .switchIfEmpty(Mono.defer(() -> {
                    UseUnit unit = UseUnit.register(code, unitName, creditCode, district,
                            contactName, contactPhone, UnitStatus.fromCode(statusCode));
                    return saveWithUniqueGuard(unit, "单位编号已存在：" + code);
                }));    }

    /** 修改档案（编号填错也支持改）；不存在报 404 语义的业务异常。 */
    public Mono<UseUnit> modify(Long id, String unitCode, String unitName, String creditCode,
                                String district, String contactName, String contactPhone,
                                String statusCode) {
        String code = unitCode.trim();
        return requireUnit(id)
                .flatMap(unit -> useUnitRepository.findByCode(code)
                        .flatMap(existing -> {
                            if (!existing.getId().equals(id)) {
                                return Mono.<UseUnit>error(new BizException("单位编号已被占用：" + code));
                            }
                            return Mono.just(unit);
                        })
                        .switchIfEmpty(Mono.just(unit)))
                .flatMap(unit -> {
                    // 在原聚合上改，保留 id 与审计字段
                    unit.modify(code, unitName, creditCode, district,
                            contactName, contactPhone, UnitStatus.fromCode(statusCode));
                    return saveWithUniqueGuard(unit, "单位编号已被占用：" + code);
                });
    }

    public Mono<UseUnit> detail(Long id) {
        return requireUnit(id);
    }

    public Mono<PageResult<UseUnit>> page(int pageNum, int pageSize,
                                          String unitCode, String unitName, String statusCode) {
        return useUnitRepository.page(pageNum, pageSize, unitCode, unitName,
                UnitStatus.fromCode(statusCode));
    }

    /** 单位不再用了就删掉；名下还有设备（家底）的不允许删，得先把设备处理干净。 */
    public Mono<Void> delete(Long id) {
        return requireUnit(id)
                .flatMap(unit -> equipmentRepository.countByUnit(id)
                        .flatMap(count -> {
                            if (count > 0) {
                                return Mono.error(new BizException(
                                        "该单位名下还有 " + count + " 台设备，不能删除，请先处理设备"));
                            }
                            return useUnitRepository.softDelete(id);
                        }));
    }

    private Mono<UseUnit> requireUnit(Long id) {
        return useUnitRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("单位不存在或已删除：id=" + id)));
    }

    /** 应用层查重之外的最后一道防线：并发下撞 uk_unit_code 时转成友好提示。 */
    private Mono<UseUnit> saveWithUniqueGuard(UseUnit unit, String message) {
        return useUnitRepository.save(unit)
                .onErrorResume(DuplicateKeyException.class,
                        e -> Mono.error(new BizException(message)));
    }
}
