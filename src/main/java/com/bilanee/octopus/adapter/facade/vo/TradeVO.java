package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.MarketStatus;
import com.bilanee.octopus.basic.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TradeVO {

    Integer roundId;
    TradeStage tradeStage;
    @Nullable
    MarketStatus marketStatus;
    Long endingTimestamp;

}
