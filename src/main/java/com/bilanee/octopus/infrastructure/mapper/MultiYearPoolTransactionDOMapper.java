package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.AdminDO;
import com.bilanee.octopus.infrastructure.entity.MultiYearPoolTransactionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MultiYearPoolTransactionDOMapper extends BaseMapper<MultiYearPoolTransactionDO> {
}
