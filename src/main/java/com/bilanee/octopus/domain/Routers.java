package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.MetaUnit;
import com.stellariver.milky.domain.support.context.Context;
import com.stellariver.milky.domain.support.event.EventRouter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@CustomLog
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Routers {

    @EventRouter
    public void route(CompEvent.Created created, Context context) {
        Comp comp = created.getComp();
        List<Map<String, List<MetaUnit>>> roundMetaUnits = created.getRoundMetaUnits();


    }

}
