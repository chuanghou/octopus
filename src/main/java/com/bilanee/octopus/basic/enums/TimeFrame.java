package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum TimeFrame {

    PEAK(1, "峰时段", Arrays.asList(16, 17, 18, 19, 20, 21)),
    FLAT(2, "平时段", Arrays.asList(6, 7, 10, 11, 14, 15, 22, 23)),
    VALLEY(3, " 谷时段", Arrays.asList(0, 1, 2, 3, 4, 5, 12, 13));

    final Integer dbCode;
    final String desc;
    final List<Integer> prds;



}
