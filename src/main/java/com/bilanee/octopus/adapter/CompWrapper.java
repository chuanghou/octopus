package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.infrastructure.entity.CompDO;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.dependency.DAOWrapper;
import com.stellariver.milky.domain.support.dependency.DaoAdapter;
import com.stellariver.milky.domain.support.dependency.DataObjectInfo;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompWrapper implements DAOWrapper<CompDO, Long> {

    final CompDOMapper compDOMapper;

    @Override
    public int batchSave(List<CompDO> compDOs) {
        return compDOs.stream().map(compDOMapper::insert).reduce(0, Integer::sum);
    }

    @Override
    public int batchUpdate(List<CompDO> compDOs) {
        return compDOs.stream().map(compDOMapper::updateById).reduce(0, Integer::sum);
    }

    @Override
    public Map<Long, CompDO> batchGetByPrimaryIds(@NonNull Set<Long> compIds) {
        return Collect.toMap(compDOMapper.selectBatchIds(compIds), CompDO::getCompId);
    }
}
