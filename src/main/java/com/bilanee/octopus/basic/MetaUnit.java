package com.bilanee.octopus.basic;

import com.baomidou.mybatisplus.annotation.TableField;
import com.bilanee.octopus.infrastructure.entity.MetaUnitDO;
import com.stellariver.milky.domain.support.command.Command;
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

    Integer metaUnitId;
    Integer nodeId;
    String name;
    Province province;
    UnitType unitType;
    @Nullable
    GeneratorType generatorType;
    Integer sourceId;
    Map<TimeFrame, Map<Direction, Double>> capacity;

}
