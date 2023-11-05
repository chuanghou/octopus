package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.IndividualLoadBasic;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoadBasicMapper extends BaseMapper<IndividualLoadBasic> {
}
