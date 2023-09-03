package com.bilanee.octopus.basic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitVO {

    Long unitId;
    Long compId;
    Integer roundId;
    String userId;
    Long metaUnitId;
    Map<TimeFrame, Map<Direction, Double>> balance;

}
