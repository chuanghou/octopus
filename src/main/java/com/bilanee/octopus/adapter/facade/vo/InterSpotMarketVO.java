package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.bilanee.octopus.PointSerialize;
import com.bilanee.octopus.basic.Point;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterSpotMarketVO {

    /**
     * 卖方申报总量
     */
    Double sellDeclaredTotal;

    /**
     * 受电省电网申报电力
     */
    Double receiverDeclaredTotal;

    /**
     * 成交电力
     */
    Double dealTotal;

    /**
     * 成交均价
     */
    Double dealAveragePrice;


    /**
     * 分时供需曲线：需求曲线
     */
    Double requireQuantity;

    /**
     * 分时供需曲线：出清价格
     */
    Double clearPrice;

    /**
     * 分时供需曲线：供给曲线
     */
    List<SpotSection> supplySections;

    /**
     * 分时供需曲线：供给曲线终点
     */
    Point<Double> supplyTerminus;

    /**
     * 分时供需曲线：需求曲线
     */
    List<SpotSection> requireSections;

    /**
     * 分时供需曲线：需求曲线终点
     */
    Point<Double> requireTerminus;
    
}
