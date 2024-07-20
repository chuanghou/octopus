package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RenewableType {

    WIND(1, "风电"), SOLAR(2, "光伏");

    final Integer dbCode;
    final String desc;

}
