package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = DoubleSerialize.class)
    Double maxDealPrice;


    /**
     * 最低成交价，单位“元/MWh”(Figma上是买方申报电力，改一下UI）
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double minDealPrice;


    /**
     * 全天最高负荷
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double maxLoad;


    /**
     * 全天最低负荷
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double minLoad;




}
