package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.IntraMarketHistoryDO;
import com.bilanee.octopus.infrastructure.entity.IntraMarketRealtimeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RealtimeMarketDOMapper extends BaseMapper<IntraMarketRealtimeDO> {
}
