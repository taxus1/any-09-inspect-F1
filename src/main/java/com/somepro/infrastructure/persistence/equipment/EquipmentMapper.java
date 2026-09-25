package com.somepro.infrastructure.persistence.equipment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.equipment.po.EquipmentPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备的 MyBatis-Plus Mapper（基础设施层）。
 *
 * BaseMapper 已提供 insert / updateById / selectById / selectList / deleteById 等能力，
 * 没有自定义 SQL 就不要加方法，也不要写 XML。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上经 BlockingJdbc 桥接调用。
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<EquipmentPO> {
}
