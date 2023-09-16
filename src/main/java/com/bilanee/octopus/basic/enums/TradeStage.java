package com.bilanee.octopus.basic.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum TradeStage {

    AN_INTER("年度省间", true, TradeType.INTER),
    AN_INTRA("年度省内", true, TradeType.INTRA),
    MO_INTER("月度省间", true, TradeType.INTER),
    MO_INTRA("月度省内", true, TradeType.INTRA),
    DA_INTRA("现货省内", true, TradeType.SPOT),
    DA_INTER("现货省间", true, TradeType.SPOT),
    END("交易结算", false, null);

    final String desc;
    final Boolean tradeable;
    final TradeType tradeType;

    static public List<TradeStage> marketStages() {
        return Arrays.stream(TradeStage.values()).filter(TradeStage::getTradeable).collect(Collectors.toList());
    }
}
