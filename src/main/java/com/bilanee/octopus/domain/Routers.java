package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.MetaUnit;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.context.Context;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import com.stellariver.milky.domain.support.event.EventRouter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

@CustomLog
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Routers {

    final UniqueIdGetter uniqueIdGetter;

    @EventRouter
    public void route(CompEvent.Created created, Context context) {
        Comp comp = created.getComp();
        List<Map<String, List<MetaUnit>>> roundMetaUnits = created.getRoundMetaUnits();
        for (int roundId = 0; roundId < roundMetaUnits.size(); roundId++) {
            Map<String, List<MetaUnit>> groupMetaUnits = roundMetaUnits.get(roundId);
            for (String userId : groupMetaUnits.keySet()) {
                List<MetaUnit> metaUnits = groupMetaUnits.get(userId);
                for (MetaUnit metaUnit : metaUnits) {
                    UnitCmd.Create command = UnitCmd.Create.builder().unitId(uniqueIdGetter.get())
                            .compId(comp.getCompId())
                            .roundId(roundId)
                            .balance(metaUnit.getCapacity())
                            .metaUnit(metaUnit)
                            .userId(userId)
                            .build();
                    CommandBus.driveByEvent(command, created);
                }
            }
        }

    }


}
