package com.bilanee.octopus.adapter.tunnel;

import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidQuery {

    Long compId;
    String userId;
    Set<Long> unitIds;
    Integer roundId;
    Province province;
    TradeStage tradeStage;
    Direction direction;
    BidStatus bidStatus;

}
