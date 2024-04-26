package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstantSpotMarketVO {

    /**
     * 日前市场成交概况
     */
    IntraSpotDealVO daIntraSpotDealVO;

    /**
     * 实时市场成交概况
     */
    IntraSpotDealVO rtIntraSpotDealVO;

    /**
     * 单元搜索信息
     */
    List<UnitVO> unitVOs;

    /**
     * 某个时间点，日前供需曲线集合
     */
    SpotMarketEntityVO daEntityVO;

    /**
     * 某个时间点，实时供需曲线集合
     */
    SpotMarketEntityVO rtEntityVO;


}
