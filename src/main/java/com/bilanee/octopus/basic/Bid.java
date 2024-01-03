package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.*;
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
    String userId;
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
    Double closeBalance;

    public Double getTransit() {
        if ((bidStatus != BidStatus.MANUAL_CANCELLED) && (bidStatus != BidStatus.SYSTEM_CANCELLED)) {
            return quantity - deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
        } else {
            return 0D;
        }
    }

    public Double getCancelled() {
        if ((bidStatus != BidStatus.MANUAL_CANCELLED) && (bidStatus != BidStatus.SYSTEM_CANCELLED)) {
            return 0D;
        } else {
            return quantity - deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
        }
    }


}
