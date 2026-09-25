package com.somepro.infrastructure.persistence.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.equipment.po.EquipmentPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备档案的 MyBatis-Plus Mapper（基础设施层）。阻塞 JDBC API，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<EquipmentPO> {
}
