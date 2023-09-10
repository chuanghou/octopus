package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BidStatus {

    NEW_DECELERATED("新单已报"),
    PART_DEAL("部分成交"),
    COMPLETE_DEAL("全部成交"),
    CANCELLED("撤单");

    final String desc;
}
