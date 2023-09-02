package com.bilanee.octopus.adapter;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.infrastructure.entity.MetaUnitDO;
import com.bilanee.octopus.infrastructure.mapper.MetaUnitDOMapper;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.RequiredArgsConstructor;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class Tunnel {

    final MetaUnitDOMapper metaUnitDOMapper;

    public Map<String, List<MetaUnit>> assignMetaUnits(Integer roundId, List<String> userIds) {
        Map<String, List<MetaUnit>> metaUnitMap = new HashMap<>();

        for (int i = 0; i < userIds.size(); i++) {
            List<Integer> sourceIds = AllocateUtils.allocateSourceId(roundId, userIds.size(), 30, i);
            LambdaQueryWrapper<MetaUnitDO> in = new LambdaQueryWrapper<MetaUnitDO>().in(MetaUnitDO::getSourceId, sourceIds);
            List<MetaUnitDO> metaUnitDOs = metaUnitDOMapper.selectList(in);
            metaUnitMap.put(userIds.get(i), Collect.transfer(metaUnitDOs, Convertor.INST::to));
        }

        return metaUnitMap;
    }


    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        MetaUnit to(MetaUnitDO metaUnitDO);


    }

}
