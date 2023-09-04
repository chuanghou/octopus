package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.Deal;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterDealVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 分设备平均成交量价：总成电力
     */
    Double totalQuantity;


    /**
     * 分设备平均成交量价：成交均价
     */
    Double averagePrice;


    /**
     * 分设备分笔成交成交量价
     */
    List<Deal> deals;
}
