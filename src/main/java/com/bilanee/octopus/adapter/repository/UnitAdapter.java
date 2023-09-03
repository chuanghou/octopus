package com.bilanee.octopus.adapter.repository;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.domain.support.dependency.DaoAdapter;
import com.stellariver.milky.domain.support.dependency.DataObjectInfo;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitAdapter implements DaoAdapter<Unit> {

    @Override
    public Unit toAggregate(@NonNull Object dataObject) {
        UnitDO unitDO = (UnitDO) dataObject;
        return Convertor.INST.to(unitDO);
    }

    @Override
    public Object toDataObject(Unit unit, DataObjectInfo dataObjectInfo) {
        return Convertor.INST.to(unit);
    }

    @Override
    public DataObjectInfo dataObjectInfo(String aggregateId) {
        return DataObjectInfo.builder().clazz(UnitDO.class).primaryId(Long.parseLong(aggregateId)).build();
    }

    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Unit to(UnitDO unitDO);

        @AfterMapping
        default void after(UnitDO unitDO, @MappingTarget Unit unit) {
            Tunnel tunnel = BeanUtil.getBean(Tunnel.class);
            MetaUnit metaUnit = tunnel.getMetaUnitById(unitDO.getMetaUnitId());
            unit.setMetaUnit(metaUnit);
        }

        @BeanMapping(builder = @Builder(disableBuilder = true))
        UnitDO to(Unit unit);

        @AfterMapping
        default void after(Unit unit, @MappingTarget UnitDO unitDO) {
            unitDO.setMetaUnitId(unit.getMetaUnit().getMetaUnitId());
        }

    }

}
