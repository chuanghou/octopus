package com.bilanee.octopus.basic.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum TradeStage {

    AN_INTER( "省间年度", TradeType.INTER, 1, 1),
    AN_INTRA( "省内年度", TradeType.INTRA, 3, 1),
    MO_INTER( "省间月度", TradeType.INTER, 2, 2),
    MO_INTRA( "省内月度", TradeType.INTRA, 4, 2),
    ROLL( "滚动撮合", TradeType.ROLL, 6, null), // 这个地方应该还需要一个定义，存储的成交结果有两个部分
    DA_INTRA( "省内现货", TradeType.SPOT, null, null),
    DA_INTER( "省间现货", TradeType.SPOT, null, null),
    END( "交易结算", null, null, null);

    final String desc;
    final TradeType tradeType;
    final Integer marketType;
    final Integer marketType2;
}
