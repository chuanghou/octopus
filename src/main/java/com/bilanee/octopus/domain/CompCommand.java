package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.CompStage;
import com.bilanee.octopus.basic.MarketStatus;
import com.bilanee.octopus.basic.TradeStage;
import com.stellariver.milky.domain.support.command.Command;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

public class CompCommand {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Create extends Command {

        Long compId;

        Integer compInitLength;
        Integer quitCompeteLength;
        Integer quitResultLength;
        Map<TradeStage, Integer> marketStageBidLengths;
        Map<TradeStage, Integer> marketStageClearLengths;
        Integer tradeResultLength;


        @Override
        public String getAggregateId() {
            return compId.toString();
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
