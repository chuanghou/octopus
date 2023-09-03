package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MarketStatus {

    BID("竞价中"),
    CLEAR("清算结果");

    final String desc;
}
