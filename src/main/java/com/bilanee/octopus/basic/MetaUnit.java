package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetaUnit {

    String name;
    Province province;
    UnitType unitType;
    GridLimit priceLimit;
    @Nullable
    GeneratorType generatorType;
    @Nullable
    RenewableType renewableType;
    Integer sourceId;
    Map<TimeFrame, Map<Direction, Double>> capacity;
    Double minCapacity;
    Double maxCapacity;

}
