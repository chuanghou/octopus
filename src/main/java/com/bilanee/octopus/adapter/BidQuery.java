package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.BidStatus;
import com.bilanee.octopus.basic.Direction;
import com.bilanee.octopus.basic.Province;
import com.bilanee.octopus.basic.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidQuery {

    Long compId;
    Long unitId;
    Integer roundId;
    Province province;
    TradeStage tradeStage;
    Direction direction;
    BidStatus bidStatus;

}
