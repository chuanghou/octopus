package com.bilanee.octopus.adapter.repository;

import com.bilanee.octopus.infrastructure.entity.CompDO;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.dependency.DAOWrapper;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompDOWrapper implements DAOWrapper<CompDO, Long> {

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
