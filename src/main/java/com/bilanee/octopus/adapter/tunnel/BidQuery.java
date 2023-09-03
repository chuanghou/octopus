package com.bilanee.octopus.adapter.tunnel;

import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TradeStage;
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
