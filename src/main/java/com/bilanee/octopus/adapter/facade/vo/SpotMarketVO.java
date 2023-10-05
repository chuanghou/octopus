package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotMarketVO {

    /**
     * 单元搜索信息
     */
    List<UnitVO> unitVOs;

    /**
     * 日前供需曲线集合，数组长度为24，代表0~23个时间点
     */
    List<SpotMarketEntityVO> daEntityVOs;

    /**
     * 日前供需曲线集合，数组长度为24，代表0~23个时间点
     */
    List<SpotMarketEntityVO> rtEntityVOs;

}
