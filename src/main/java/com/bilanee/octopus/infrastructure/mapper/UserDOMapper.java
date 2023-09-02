package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.CompDO;
import com.bilanee.octopus.infrastructure.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDOMapper extends BaseMapper<UserDO> {
}
