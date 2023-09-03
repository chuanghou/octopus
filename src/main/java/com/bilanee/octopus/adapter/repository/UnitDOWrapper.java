package com.bilanee.octopus.adapter.repository;

import com.bilanee.octopus.infrastructure.entity.UnitDO;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
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
public class UnitDOWrapper implements DAOWrapper<UnitDO, Long> {

    final UnitDOMapper unitDOMapper;

    @Override
    public int batchSave(List<UnitDO> unitDOs) {
        return unitDOs.stream().map(unitDOMapper::insert).reduce(0, Integer::sum);
    }

    @Override
    public int batchUpdate(List<UnitDO> unitDOs) {
        return unitDOs.stream().map(unitDOMapper::updateById).reduce(0, Integer::sum);
    }

    @Override
    public Map<Long, UnitDO> batchGetByPrimaryIds(@NonNull Set<Long> compIds) {
        return Collect.toMap(unitDOMapper.selectBatchIds(compIds), UnitDO::getUnitId);
    }
}
