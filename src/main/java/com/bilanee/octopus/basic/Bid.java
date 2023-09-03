package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.basic.enums.Direction;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
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
    Integer roundId;
    TradeStage tradeStage;
    Direction direction;
    Double quantity;
    Double price;
    Long declareTimeStamp;
    @Builder.Default
    List<Deal> deals = new ArrayList<>();
    Long cancelledTimeStamp;
    BidStatus bidStatus;

}
