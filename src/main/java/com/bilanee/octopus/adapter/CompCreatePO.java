package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompCreatePO {

    Integer compInitLength;
    Integer quitCompeteLength;
    Integer quitResultLength;
    Map<TradeStage, Integer> marketStageBidLengths;
    Map<TradeStage, Integer> marketStageClearLengths;
    Integer tradeResultLength;

}
