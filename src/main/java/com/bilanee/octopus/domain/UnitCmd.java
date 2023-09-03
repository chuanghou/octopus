package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.*;
import com.stellariver.milky.domain.support.command.Command;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
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
        Long compId;
        Integer roundId;
        String userId;
        MetaUnit metaUnit;
        Map<TimeFrame, Map<Direction, Double>> balance;

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

        StageId stageId;
        List<Bid> bids;

        @Override
        public String getAggregateId() {
            return bids.get(0).getUnitId().toString();
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
