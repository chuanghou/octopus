package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.BidDO;
import com.bilanee.octopus.infrastructure.entity.TransLimitDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransLimitDOMapper extends BaseMapper<TransLimitDO> {
}
