package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.stellariver.milky.domain.support.base.BaseDataObject;
import com.stellariver.milky.infrastructure.base.database.AbstractMpDO;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_unit_do", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitDO extends AbstractMpDO implements BaseDataObject<Long> {

    @TableId(type = IdType.INPUT)
    Long unitId;
    Long compId;
    Integer roundId;
    String userId;
    Long metaUnitId;
    String unitName;
    @TableField(typeHandler = PositionHandler.class)
    Map<TimeFrame, Map<Direction, Double>> balance;


    @Override
    public Long getPrimaryId() {
        return unitId;
    }


}
