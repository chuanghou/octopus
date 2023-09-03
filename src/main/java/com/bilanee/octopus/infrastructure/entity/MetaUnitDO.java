package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_meta_unit_do", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetaUnitDO {

    @TableId(type = IdType.INPUT)
    Integer metaUnitId;
    Integer nodeId;
    String name;
    Province province;
    UnitType unitType;
    @Nullable
    GeneratorType generatorType;
    Integer sourceId;
    @TableField(typeHandler = PositionHandler.class)
    Map<TimeFrame, Map<Direction, Double>> capacity;

}
