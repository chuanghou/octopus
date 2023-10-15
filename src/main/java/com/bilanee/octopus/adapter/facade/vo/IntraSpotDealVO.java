package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraSpotDealVO {

    /**
     * 最高成交价，单位“元/MWh”(Figma上是卖方申报电力，改一下UI）
     */
    Double maxDealPrice;


    /**
     * 最低成交价，单位“元/MWh”(Figma上是买方申报电力，改一下UI）
     */
    Double minDealPrice;


    /**
     * 全天最高负荷
     */
    Double maxLoad;


    /**
     * 全天最低负荷
     */
    Double minLoad;




}
