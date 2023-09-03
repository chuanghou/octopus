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

    AN_INTER("年度省间", true),
    AN_INTRA("年度省内", true),
    MO_INTER("月度省间", true),
    MO_INTRA("月度省内", true),
    DA_INTRA("现货省内", true),
    DA_INTER("现货省间", true),
    END("交易结算", false);

    final String desc;
    final Boolean tradeable;

    static public List<TradeStage> marketStages() {
        return Arrays.stream(TradeStage.values()).filter(TradeStage::getTradeable).collect(Collectors.toList());
    }
}
