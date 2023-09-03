package com.bilanee.octopus.adapter.repository;

import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.infrastructure.entity.CompDO;
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
public class CompAdapter implements DaoAdapter<Comp> {

    @Override
    public Comp toAggregate(@NonNull Object dataObject) {
        CompDO compDO = (CompDO) dataObject;

        return Convertor.INST.to(compDO);
    }

    @Override
    public Object toDataObject(Comp comp, DataObjectInfo dataObjectInfo) {
        return Convertor.INST.to(comp);
    }

    @Override
    public DataObjectInfo dataObjectInfo(String aggregateId) {
        return DataObjectInfo.builder().clazz(CompDO.class).primaryId(Long.parseLong(aggregateId)).build();
    }

    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Comp to(CompDO compDO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompDO to(Comp comp);

    }

}
