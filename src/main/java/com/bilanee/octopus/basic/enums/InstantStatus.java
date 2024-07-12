package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InstantStatus {

    NOT_BID("尚未挂牌"),
    NOT_DEALT("尚未成交"),
    PART_DEALT("部分成交"),
    ALL_DEALT("全部成交"),
    PART_DEALT_PART_CANCELLED("部成部撤"),
    SYSTEM_CANCELLED("系统撤单");

    final String desc;
}
