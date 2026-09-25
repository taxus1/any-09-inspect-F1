package com.somepro.infrastructure.persistence.equipment;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.equipment.model.DeviceType;
import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.equipment.model.EquipmentStatus;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.equipment.repository.EquipmentRepository;
import com.somepro.infrastructure.persistence.equipment.converter.EquipmentPoConverter;
import com.somepro.infrastructure.persistence.equipment.po.EquipmentPO;
import com.somepro.infrastructure.persistence.support.BlockingJdbc;
import com.somepro.infrastructure.persistence.useunit.po.UseUnitPO;
import com.somepro.infrastructure.persistence.useunit.UseUnitMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备仓储适配器：用 MyBatis-Plus 实现领域仓储端口（基础设施层）。
 *
 * - 所有 DB 调用都经 {@link BlockingJdbc#blocking} 桥接到 boundedElastic，绝不在 event-loop 上跑 JDBC。
 * - 软删除交给 @TableLogic；分页统一用 PageHelper。
 * - 分页结果里的 unitName（归属单位名称）在本适配器内按 unit_id 批量回填 ——
 *   列表是先分页后的当前页，IN 数量等于 pageSize，不会随表变大。
 */
@Repository
public class EquipmentRepositoryImpl implements EquipmentRepository {

    private final EquipmentMapper equipmentMapper;
    private final UseUnitMapper useUnitMapper;

    public EquipmentRepositoryImpl(EquipmentMapper equipmentMapper, UseUnitMapper useUnitMapper) {
        this.equipmentMapper = equipmentMapper;
        this.useUnitMapper = useUnitMapper;
    }

    @Override
    public Mono<Equipment> save(Equipment equipment) {
        return BlockingJdbc.blocking(() -> {
            EquipmentPO po = EquipmentPoConverter.toPo(equipment);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                equipmentMapper.insert(po);
            } else {
                equipmentMapper.updateById(po);
            }
            // 登记/改档返回也带上归属单位名称，和详情/列表保持一致
            fillUnitNames(List.of(po));
            return EquipmentPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Equipment> findById(Long id) {
        return BlockingJdbc.blocking(() -> {
            EquipmentPO po = equipmentMapper.selectById(id);
            if (po == null) {
                return null;
            }
            fillUnitNames(List.of(po));
            return EquipmentPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Equipment> findByRegCode(String regCode) {
        return BlockingJdbc.blocking(() -> {
            EquipmentPO po = equipmentMapper.selectOne(Wrappers.<EquipmentPO>lambdaQuery()
                    .eq(EquipmentPO::getRegCode, regCode)
                    .last("limit 1"));
            return po == null ? null : EquipmentPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<Equipment>> page(int pageNum, int pageSize, String regCode, Long unitId,
                                            DeviceType type, EquipmentStatus status) {
        return BlockingJdbc.<PageResult<Equipment>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<EquipmentPO> wrapper = Wrappers.<EquipmentPO>lambdaQuery();
                if (regCode != null && !regCode.isBlank()) {
                    wrapper.like(EquipmentPO::getRegCode, regCode.trim());
                }
                if (unitId != null) {
                    wrapper.eq(EquipmentPO::getUnitId, unitId);
                }
                if (type != null) {
                    wrapper.eq(EquipmentPO::getDeviceType, type.name());
                }
                if (status != null) {
                    wrapper.eq(EquipmentPO::getStatus, status.name());
                }
                wrapper.orderByDesc(EquipmentPO::getId);
                List<EquipmentPO> rows = equipmentMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                fillUnitNames(rows);
                List<Equipment> content = rows.stream()
                        .map(EquipmentPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // 分页参数靠 ThreadLocal 传递，必须清理，否则污染线程池里的下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Long> countByUnitId(Long unitId) {
        return BlockingJdbc.blocking(() ->
                equipmentMapper.selectCount(Wrappers.<EquipmentPO>lambdaQuery()
                        .eq(EquipmentPO::getUnitId, unitId)));
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return BlockingJdbc.blocking(() -> {
            // @TableLogic 翻译成 UPDATE t_equipment SET del_flag = 1 WHERE id = ? AND del_flag = 0
            equipmentMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    /**
     * 按当前页设备上的 unit_id 批量查单位名并回填（IN 数量 = 页大小）。
     * 单位被物理清掉等极端情况下 unitName 保持 null，不影响设备记录返回。
     */
    private void fillUnitNames(List<EquipmentPO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> unitIds = rows.stream()
                .map(EquipmentPO::getUnitId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (unitIds.isEmpty()) {
            return;
        }
        List<UseUnitPO> units = useUnitMapper.selectList(Wrappers.<UseUnitPO>lambdaQuery()
                .in(UseUnitPO::getId, unitIds));
        Map<Long, String> nameMap = units.stream()
                .collect(Collectors.toMap(UseUnitPO::getId, UseUnitPO::getUnitName, (a, b) -> a));
        rows.forEach(row -> row.setUnitName(nameMap.get(row.getUnitId())));
    }
}
