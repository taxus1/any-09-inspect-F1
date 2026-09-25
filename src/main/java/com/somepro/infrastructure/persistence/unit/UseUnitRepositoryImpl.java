package com.somepro.infrastructure.persistence.unit;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.model.UnitStatus;
import com.somepro.domain.unit.model.UseUnit;
import com.somepro.domain.unit.repository.UseUnitRepository;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.equipment.mapper.EquipmentMapper;
import com.somepro.infrastructure.persistence.equipment.po.EquipmentPO;
import com.somepro.infrastructure.persistence.unit.converter.UseUnitPoConverter;
import com.somepro.infrastructure.persistence.unit.po.UseUnitPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 使用单位仓储适配器（基础设施层）：MyBatis-Plus 阻塞 JDBC 经 {@link #blocking} 桥进响应式链路。
 *
 * 「单位名下设备数」直接用 {@link EquipmentMapper} 按 unit_id 统计（@TableLogic 自动带 del_flag=0）。
 */
@Repository
public class UseUnitRepositoryImpl implements UseUnitRepository {

    private final UseUnitMapper useUnitMapper;
    private final EquipmentMapper equipmentMapper;

    public UseUnitRepositoryImpl(UseUnitMapper useUnitMapper, EquipmentMapper equipmentMapper) {
        this.useUnitMapper = useUnitMapper;
        this.equipmentMapper = equipmentMapper;
    }

    @Override
    public Mono<UseUnit> save(UseUnit unit) {
        return blocking(() -> {
            UseUnitPO po = UseUnitPoConverter.toPo(unit);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                useUnitMapper.insert(po);
            } else {
                useUnitMapper.updateById(po);
            }
            return UseUnitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<UseUnit> findById(Long id) {
        return blocking(() -> {
            UseUnitPO po = useUnitMapper.selectById(id);
            return po == null ? null : UseUnitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<UseUnit> findByCode(String unitCode) {
        return blocking(() -> {
            UseUnitPO po = useUnitMapper.selectOne(
                    Wrappers.<UseUnitPO>lambdaQuery().eq(UseUnitPO::getUnitCode, unitCode.trim()));
            return po == null ? null : UseUnitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<UseUnit>> page(int pageNum, int pageSize, String unitCode,
                                          String unitName, UnitStatus status) {
        return this.<PageResult<UseUnit>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<UseUnitPO> wrapper = Wrappers.<UseUnitPO>lambdaQuery()
                        .orderByDesc(UseUnitPO::getId);
                if (unitCode != null && !unitCode.isBlank()) {
                    wrapper.like(UseUnitPO::getUnitCode, unitCode.trim());
                }
                if (unitName != null && !unitName.isBlank()) {
                    wrapper.like(UseUnitPO::getUnitName, unitName.trim());
                }
                if (status != null) {
                    wrapper.eq(UseUnitPO::getStatus, status.name());
                }
                List<UseUnitPO> rows = useUnitMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<UseUnit> content = rows.stream()
                        .map(UseUnitPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Long> countEquipment(Long unitId) {
        return blocking(() -> equipmentMapper.selectCount(
                Wrappers.<EquipmentPO>lambdaQuery().eq(EquipmentPO::getUnitId, unitId)));
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return blocking(() -> {
            useUnitMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
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
