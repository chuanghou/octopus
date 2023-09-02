package com.bilanee.octopus.basic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bid {

    Long bidId;
    Long compId;
    Long unitId;
    Province province;
    TimeFrame timeFrame;
    TradeStage tradeStage;
    Direction direction;
    Double quantity;
    Double price;
    Long declareTimeStamp;
    List<Deal> deals;
    Date cancelledTimeStamp;
    BidStatus bidStatus;

}
