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

    AN_INTER(1, "省间年度", true, TradeType.INTER),
    AN_INTRA(2, "省内年度", true, TradeType.INTRA),
    MO_INTER(3, "省间月度", true, TradeType.INTER),
    MO_INTRA(4, "省内月度", true, TradeType.INTRA),
    DA_INTRA(5, "省内现货", true, TradeType.SPOT),
    DA_INTER(6, "省间现货", true, TradeType.SPOT),
    END(7, "交易结算", false, null);

    final Integer dbCode;
    final String desc;
    final Boolean tradeable;
    final TradeType tradeType;

    static public List<TradeStage> marketStages() {
        return Arrays.stream(TradeStage.values()).filter(TradeStage::getTradeable).collect(Collectors.toList());
    }
}
