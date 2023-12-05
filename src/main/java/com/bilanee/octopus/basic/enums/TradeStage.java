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

    AN_INTER(1, "省间年度", true, TradeType.INTER, 1),
    AN_INTRA(2, "省内年度", true, TradeType.INTRA, 2),
    MO_INTER(3, "省间月度", true, TradeType.INTER, 3),
    MO_INTRA(4, "省内月度", true, TradeType.INTRA, 4),
    DA_INTRA(5, "省内现货", true, TradeType.SPOT, 5),
    DA_INTER(6, "省间现货", true, TradeType.SPOT, 6),
    END(7, "交易结算", false, null, null);

    final Integer dbCode;
    final String desc;
    final Boolean tradeable;
    final TradeType tradeType;
    final Integer marketType;

    static public List<TradeStage> marketStages() {
        return Arrays.stream(TradeStage.values()).filter(TradeStage::getTradeable).collect(Collectors.toList());
    }
}
