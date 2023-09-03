package com.bilanee.octopus.domain;

import com.bilanee.octopus.common.enums.CompStage;
import com.bilanee.octopus.common.enums.MarketStatus;
import com.bilanee.octopus.common.enums.TradeStage;
import com.stellariver.milky.domain.support.command.Command;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

public class CompCmd {

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
        List<String> userIds;


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


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Clear extends Command {

        Long compId;

        @Override
        public String getAggregateId() {
            return compId.toString();
        }

    }
}
