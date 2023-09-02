package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.CompCreatePO;
import com.bilanee.octopus.adapter.CompFacade;
import com.bilanee.octopus.adapter.CompVO;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.Direction;
import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.infrastructure.entity.MetaUnitDO;
import com.bilanee.octopus.basic.TimeFrame;
import com.stellariver.milky.domain.support.base.AggregateRoot;
import com.stellariver.milky.domain.support.command.ConstructorHandler;
import com.stellariver.milky.domain.support.command.MethodHandler;
import com.stellariver.milky.domain.support.context.Context;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.Builder;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Unit extends AggregateRoot {

    Long unitId;
    Long compId;
    Integer roundId;
    String userId;
    MetaUnit metaUnit;
    Map<TimeFrame, Map<Direction, Double>> balance;

    @Override
    public String getAggregateId() {
        return unitId.toString();
    }


    @ConstructorHandler
    public static Unit create(UnitCmd.Create command, Context context) {
        Unit unit = Convertor.INST.to(command);
        context.publishPlaceHolderEvent(unit.getAggregateId());
        return unit;
    }


    @MethodHandler
    public void handle(UnitCmd.CentralizedBids command, Context context) {

    }



    @MethodHandler
    public void handle(UnitCmd.RealtimeBid command, Context context) {

    }


    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Unit to(UnitCmd.Create command);


    }
}
