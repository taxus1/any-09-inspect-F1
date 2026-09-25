package com.somepro.infrastructure.persistence.useunit;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.useunit.model.UseUnit;
import com.somepro.domain.useunit.repository.UseUnitRepository;
import com.somepro.infrastructure.persistence.support.BlockingJdbc;
import com.somepro.infrastructure.persistence.useunit.converter.UseUnitPoConverter;
import com.somepro.infrastructure.persistence.useunit.po.UseUnitPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 使用单位仓储适配器：用 MyBatis-Plus 实现领域仓储端口（基础设施层）。
 *
 * - 所有 DB 调用都经 {@link BlockingJdbc#blocking} 桥接到 boundedElastic，绝不在 event-loop 上跑 JDBC。
 * - 软删除交给 @TableLogic：查询自动带 del_flag = 0，deleteById 自动改写为置 1。
 * - 分页统一用 PageHelper.startPage()，用完必须 PageHelper.clearPage()。
 */
@Repository
public class UseUnitRepositoryImpl implements UseUnitRepository {

    private final UseUnitMapper useUnitMapper;

    public UseUnitRepositoryImpl(UseUnitMapper useUnitMapper) {
        this.useUnitMapper = useUnitMapper;
    }

    @Override
    public Mono<UseUnit> save(UseUnit unit) {
        return BlockingJdbc.blocking(() -> {
            UseUnitPO po = UseUnitPoConverter.toPo(unit);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                useUnitMapper.insert(po);
            } else {
                useUnitMapper.updateById(po);
            }
            // insert 后框架回填 id 与审计字段，转回领域对象一并返回
            return UseUnitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<UseUnit> findById(Long id) {
        return BlockingJdbc.blocking(() -> {
            UseUnitPO po = useUnitMapper.selectById(id);
            return po == null ? null : UseUnitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<UseUnit> findByCode(String unitCode) {
        return BlockingJdbc.blocking(() -> {
            UseUnitPO po = useUnitMapper.selectOne(Wrappers.<UseUnitPO>lambdaQuery()
                    .eq(UseUnitPO::getUnitCode, unitCode)
                    .last("limit 1"));
            return po == null ? null : UseUnitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<UseUnit>> page(int pageNum, int pageSize, String unitName, String unitCode) {
        return BlockingJdbc.<PageResult<UseUnit>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<UseUnitPO> wrapper = Wrappers.<UseUnitPO>lambdaQuery();
                if (unitName != null && !unitName.isBlank()) {
                    wrapper.like(UseUnitPO::getUnitName, unitName.trim());
                }
                if (unitCode != null && !unitCode.isBlank()) {
                    wrapper.like(UseUnitPO::getUnitCode, unitCode.trim());
                }
                wrapper.orderByDesc(UseUnitPO::getId);
                List<UseUnitPO> rows = useUnitMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<UseUnit> content = rows.stream()
                        .map(UseUnitPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // 分页参数靠 ThreadLocal 传递，必须清理，否则污染线程池里的下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return BlockingJdbc.blocking(() -> {
            // @TableLogic 翻译成 UPDATE t_use_unit SET del_flag = 1 WHERE id = ? AND del_flag = 0
            useUnitMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }
}
