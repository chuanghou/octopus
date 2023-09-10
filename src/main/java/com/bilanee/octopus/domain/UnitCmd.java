package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
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
    static public class InterBids extends Command {

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
    static public class IntraBid extends Command {

        StageId stageId;
        Bid bid;

        @Override
        public String getAggregateId() {
            return bid.getUnitId().toString();
        }

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IntraCancel extends Command{

        Long unitId;
        Long cancelBidId;

        @Override
        public String getAggregateId() {
            return unitId.toString();
        }
    }

}
