package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.SprDO;
import com.bilanee.octopus.infrastructure.entity.UnmetDemand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UnmetDemandMapper extends BaseMapper<UnmetDemand> {
}
