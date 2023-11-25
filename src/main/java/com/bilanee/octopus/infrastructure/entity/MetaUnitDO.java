package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_meta_unit_do", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetaUnitDO {

    String name;
    Province province;
    UnitType unitType;
    @Nullable
    GeneratorType generatorType;
    Integer sourceId;
    @TableField(typeHandler = PositionHandler.class)
    Map<TimeFrame, Map<Direction, Double>> capacity;
    Double minCapacity;
    Double maxCapacity;
    Double minOutputPrice;
}
