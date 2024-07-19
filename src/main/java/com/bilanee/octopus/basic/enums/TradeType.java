package com.bilanee.octopus.basic.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum TradeType {

    MULTI("多年竞价"),
    INTER("中长期省间竞价"),
    INTRA("中长期省内竞价"),
    ROLL("日前滚动竞价"),
    SPOT("实时/日前现货");

    String desc;
}
