package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.CompStage;
import com.bilanee.octopus.basic.MarketStatus;
import com.bilanee.octopus.basic.TradeStage;
import com.stellariver.milky.domain.support.command.Command;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

public class UnitCmd {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Create extends Command {

        Long unitId;


        @Override
        public String getAggregateId() {
            return unitId.toString();
        }

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class CentralizedBids extends Command {

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
    static public class RealtimeBid extends Command {

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
