package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.CompDO;
import com.bilanee.octopus.infrastructure.entity.ThermalCostDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ThermalCostDOMapper extends BaseMapper<ThermalCostDO> {
}
