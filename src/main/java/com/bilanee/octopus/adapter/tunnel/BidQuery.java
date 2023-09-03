package com.bilanee.octopus.adapter.tunnel;

import com.bilanee.octopus.common.enums.BidStatus;
import com.bilanee.octopus.basic.Direction;
import com.bilanee.octopus.common.enums.Province;
import com.bilanee.octopus.common.enums.TradeStage;
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
