package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.bilanee.octopus.basic.Deal;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitDealVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 分设备平均成交量价：总成电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double totalQuantity;


    /**
     * 分设备平均成交量价：成交均价
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double averagePrice;


    /**
     * 分设备分笔成交成交量价
     */
    List<Deal> deals;
}
