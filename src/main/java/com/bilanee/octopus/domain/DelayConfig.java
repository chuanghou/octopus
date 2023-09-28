package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.enums.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelayConfig {

    Integer quitCompeteLength;
    Integer quitResultLength;
    Map<TradeStage, Integer> marketStageBidLengths;
    Map<TradeStage, Integer> marketStageClearLengths;
    Integer tradeResultLength;

}
