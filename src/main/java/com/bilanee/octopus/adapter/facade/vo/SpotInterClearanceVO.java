package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotInterClearanceVO {

    /**
     * 成交电价
     */
    List<Double> dealPrices;

    /**
     * 市场成交总量
     */
    List<Double> dealTotals;


    /**
     * 代理的机组成交总量
     */
    Map<String, List<Double>> generatorDeals;

}
