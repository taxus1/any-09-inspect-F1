package com.somepro.infrastructure.persistence.equipment;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.equipment.repository.EquipmentRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.equipment.converter.EquipmentPoConverter;
import com.somepro.infrastructure.persistence.equipment.mapper.EquipmentMapper;
import com.somepro.infrastructure.persistence.equipment.po.EquipmentPO;
import com.somepro.infrastructure.persistence.unit.po.UseUnitPO;
import com.somepro.infrastructure.persistence.unit.UseUnitMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 设备档案仓储适配器（基础设施层）：MyBatis-Plus 阻塞 JDBC 经 {@link #blocking} 桥进响应式链路。
 *
 * 列表按「归属单位名称」搜时，先在 t_use_unit 上模糊查一批单位 id，再用 in 过滤设备；
 * 单位一个都没命中时直接返回空页，避免拼出 {@code in ()} 非法 SQL。
 * 分页查询结束后批量把单位编号/名称回填到领域对象的快照字段。
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
        return blocking(() -> {
            EquipmentPO po = EquipmentPoConverter.toPo(equipment);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                equipmentMapper.insert(po);
            } else {
                equipmentMapper.updateById(po);
            }
            return EquipmentPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Equipment> findById(Long id) {
        return blocking(() -> {
            EquipmentPO po = equipmentMapper.selectById(id);
            if (po == null) {
                return null;
            }
            Equipment domain = EquipmentPoConverter.toDomain(po);
            fillUnitSnapshot(List.of(domain));
            return domain;
        });
    }

    @Override
    public Mono<Equipment> findByRegCode(String regCode) {
        return blocking(() -> {
            EquipmentPO po = equipmentMapper.selectOne(
                    Wrappers.<EquipmentPO>lambdaQuery().eq(EquipmentPO::getRegCode, regCode.trim()));
            if (po == null) {
                return null;
            }
            Equipment domain = EquipmentPoConverter.toDomain(po);
            fillUnitSnapshot(List.of(domain));
            return domain;
        });
    }

    @Override
    public Mono<Long> countByUnit(Long unitId) {
        return blocking(() -> equipmentMapper.selectCount(
                Wrappers.<EquipmentPO>lambdaQuery().eq(EquipmentPO::getUnitId, unitId)));
    }

    @Override
    public Mono<PageResult<Equipment>> page(int pageNum, int pageSize, String regCode,
                                            String unitName, Long unitId) {
        return this.<PageResult<Equipment>>blocking(() -> {
            // 按归属单位名称过滤：先查单位 id（这一步必须在 startPage 之前，否则会被分页插件拦到）
            List<Long> matchedUnitIds = null;
            if (unitName != null && !unitName.isBlank()) {
                List<UseUnitPO> units = useUnitMapper.selectList(
                        Wrappers.<UseUnitPO>lambdaQuery()
                                .select(UseUnitPO::getId)
                                .like(UseUnitPO::getUnitName, unitName.trim()));
                matchedUnitIds = units.stream().map(UseUnitPO::getId).collect(Collectors.toList());
                if (matchedUnitIds.isEmpty()) {
                    return new PageResult<>(Collections.emptyList(), 0, pageNum, pageSize);
                }
            }

            List<EquipmentPO> rows;
            long total;
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<EquipmentPO> wrapper = Wrappers.<EquipmentPO>lambdaQuery()
                        .orderByDesc(EquipmentPO::getId);
                if (regCode != null && !regCode.isBlank()) {
                    wrapper.like(EquipmentPO::getRegCode, regCode.trim());
                }
                if (unitId != null) {
                    wrapper.eq(EquipmentPO::getUnitId, unitId);
                }
                if (matchedUnitIds != null) {
                    wrapper.in(EquipmentPO::getUnitId, matchedUnitIds);
                }
                rows = equipmentMapper.selectList(wrapper);
                total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
            } finally {
                PageHelper.clearPage();
            }

            List<Equipment> content = rows.stream()
                    .map(EquipmentPoConverter::toDomain)
                    .collect(Collectors.toList());
            fillUnitSnapshot(content);
            return new PageResult<>(content, total, pageNum, pageSize);
        });
    }

    /** 按当前页设备的 unit_id 批量查单位，回填编号/名称快照（避免逐行 N+1）。 */
    private void fillUnitSnapshot(List<Equipment> content) {
        if (content.isEmpty()) {
            return;
        }
        List<Long> unitIds = content.stream()
                .map(Equipment::getUnitId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UseUnitPO> units = useUnitMapper.selectBatchIds(unitIds).stream()
                .collect(Collectors.toMap(UseUnitPO::getId, u -> u));
        for (Equipment equipment : content) {
            UseUnitPO unit = units.get(equipment.getUnitId());
            if (unit != null) {
                equipment.setUnitCode(unit.getUnitCode());
                equipment.setUnitName(unit.getUnitName());
            }
        }
    }

    /** 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切 boundedElastic 跑 JDBC。 */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
