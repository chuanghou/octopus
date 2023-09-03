package com.bilanee.octopus.domain;

import com.bilanee.octopus.common.enums.CompStage;
import com.bilanee.octopus.common.enums.MarketStatus;
import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.common.enums.TradeStage;
import com.stellariver.milky.domain.support.command.Command;
import com.stellariver.milky.domain.support.event.Event;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

public class CompEvent {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Created extends Event {

        Comp comp;
        List<Map<String, List<MetaUnit>>> roundMetaUnits;

        @Override
        public String getAggregateId() {
            return comp.getCompId().toString();
        }

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Step extends Command {

        Long compId;

        CompStage compStage;
        Integer roundId;
        TradeStage tradeStage;
        MarketStatus marketStatus;

        Long endingTimeStamp;

        @Override
        public String getAggregateId() {
            return compId.toString();
        }

    }
}
