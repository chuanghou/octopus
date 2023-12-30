package com.bilanee.octopus.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilanee.octopus.infrastructure.entity.AnswerDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnswerDOMapper extends BaseMapper<AnswerDO> {

    int insertOrUpdateOnDuplicate(AnswerDO answerDO);

}
