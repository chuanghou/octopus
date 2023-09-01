package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.CompStage;
import com.bilanee.octopus.basic.MarketStatus;
import com.bilanee.octopus.basic.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompVO {

    Long compId;
    CompStage compStage;
    Integer roundId;
    TradeStage tradeStage;
    MarketStatus marketStatus;
    Long endingTimeStamp;

}
