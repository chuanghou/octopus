package com.bilanee.octopus.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GeneratorType {

    CLASSIC(1, "传统"), RENEWABLE(2, "新能源");

    final Integer dbCode;
    final String desc;

}
