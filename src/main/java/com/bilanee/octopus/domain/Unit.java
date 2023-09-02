package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.Direction;
import com.bilanee.octopus.infrastructure.entity.MetaUnitDO;
import com.bilanee.octopus.basic.TimeFrame;
import com.stellariver.milky.domain.support.base.AggregateRoot;
import com.stellariver.milky.domain.support.command.ConstructorHandler;
import com.stellariver.milky.domain.support.command.MethodHandler;
import com.stellariver.milky.domain.support.context.Context;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Unit extends AggregateRoot {

    Long unitId;

    Integer userId;
    MetaUnitDO metaUnitDO;
    Map<TimeFrame, Map<Direction, Double>> balance;

    @Override
    public String getAggregateId() {
        return unitId.toString();
    }


    @ConstructorHandler
    public static Unit create(UnitCmd.Create command, Context context) {
        return null;
    }


    @MethodHandler
    public void handle(UnitCmd.CentralizedBids command, Context context) {

    }



    @MethodHandler
    public void handle(UnitCmd.RealtimeBid command, Context context) {

    }


}
