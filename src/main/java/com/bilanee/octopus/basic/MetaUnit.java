package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.UnitType;
import com.bilanee.octopus.basic.enums.GeneratorType;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.Direction;
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
    @Nullable
    GeneratorType generatorType;
    Integer sourceId;
    Map<TimeFrame, Map<Direction, Double>> capacity;

}
