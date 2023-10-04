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

    Long metaUnitId;
    Integer nodeId;
    String name;
    Province province;
    UnitType unitType;
    GridLimit priceLimit;
    @Nullable
    GeneratorType generatorType;
    Integer sourceId;
    Map<TimeFrame, Map<Direction, Double>> capacity;
    Double minCapacity;
    Double maxCapacity;
    Double minCost;
    String nameType;


    public Double minOutputPrice() {
        return minCost/minCapacity;
    }

}
