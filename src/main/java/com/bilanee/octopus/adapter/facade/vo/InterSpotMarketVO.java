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
    @JsonSerialize(using = DoubleSerialize.class)
    Double sellDeclaredTotal;

    /**
     * 受电省电网申报电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double receiverDeclaredTotal;

    /**
     * 成交电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double dealTotal;

    /**
     * 成交均价
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double dealAveragePrice;


    /**
     * 分时供需曲线：需求曲线
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double requireQuantity;

    /**
     * 分时供需曲线：出清价格, 废弃
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double clearPrice;

    /**
     * 分时供需曲线：出清价格起点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> clearLineLeft;

    /**
     * 分时供需曲线：出清价格终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> clearLineRight;

    /**
     * 分时供需曲线：供给曲线
     */
    List<SpotSection> supplySections;

    /**
     * 分时供需曲线：供给曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> supplyTerminus;

    /**
     * 分时供需曲线：需求曲线
     */
    List<SpotSection> requireSections;

    /**
     * 分时供需曲线：需求曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> requireTerminus;
    
}
