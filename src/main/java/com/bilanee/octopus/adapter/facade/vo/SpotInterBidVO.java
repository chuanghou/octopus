package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.enums.GeneratorType;
import com.bilanee.octopus.basic.enums.UnitType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotInterBidVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 单元类型
     */
    UnitType unitType;

    /**
     * 机组类型
     */
    GeneratorType generatorType;

    /**
     * 单元名称
     */
    String unitName;

    Integer sourceId;

    /**
     * 分时刻量价
     */
    List<InstantSpotBidVO> instantSpotBidVOs;

    /**
     * 价格限制
     */
    GridLimit priceLimit;

}
