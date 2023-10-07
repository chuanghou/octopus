package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.SpotLoadCleared;
import com.bilanee.octopus.infrastructure.entity.SpotUnitCleared;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SpotLoadClearedMapper extends BaseMapper<SpotLoadCleared> {
}
