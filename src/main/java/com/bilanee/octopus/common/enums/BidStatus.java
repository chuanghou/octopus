package com.bilanee.octopus.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BidStatus {

    NEW_DECELERATED("新单已报"),
    PART_DEAL("部分成交"),
    COMPLETE_DEAL("全部成交"),
    CANCEL_DECELERATED("撤单已报"),
    CANCELLED("撤单");

    final String desc;
}
