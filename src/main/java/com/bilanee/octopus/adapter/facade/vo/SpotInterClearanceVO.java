package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoublesSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> dealPrices;

    /**
     * 市场成交总量
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> dealTotals;


    /**
     * 代理的机组成交总量
     */
    Map<String, List<Double>> generatorDeals;

}
