package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterSpotUnitDealVO {

    /**
     * 单元名称
     */
    String unitName;


    /**
     * 分时成交量价
     */
    List<Deal> instantDeals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Deal {
        Double quantity;
        Double price;
    }
}

