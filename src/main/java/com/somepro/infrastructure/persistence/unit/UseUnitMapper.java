package com.somepro.infrastructure.persistence.unit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.unit.po.UseUnitPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 使用单位的 MyBatis-Plus Mapper（基础设施层）。阻塞 JDBC API，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface UseUnitMapper extends BaseMapper<UseUnitPO> {
}
