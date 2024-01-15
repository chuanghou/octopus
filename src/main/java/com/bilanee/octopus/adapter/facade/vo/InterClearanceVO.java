package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.bilanee.octopus.PointSerialize;
import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.Section;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterClearanceVO {

    /**
     * 运行时段
     */
    TimeFrame timeFrame;

    /**
     *  市场成交情况：卖方总申报电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double sellDeclaredQuantity;

    /**
     *  市场成交情况：买方总申报电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double buyDeclaredQuantity;

    /**
     *  市场成交情况：成交电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double dealQuantity;


    /**
     *  市场成交情况：成交价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double dealPrice;

    /**
     * 市场供需曲线/分设备成交量价/分设备分笔成交量价：单元情况
     */
    List<UnitVO> unitVOs;

    /**
     * 市场供需曲线：传输限制
     */
    GridLimit transLimit;


    /**
     * 市场供需曲线：需求曲线
     */
    List<Section> buySections;

    /**
     * 市场供需曲线：需求曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> buyTerminus;

    /**
     * 市场供需曲线：供给曲线
     */
    List<Section> sellSections;

    /**
     * 市场供需曲线：供给曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> sellTerminus;


    /**
     * 分设备平均成交量价/分设备分笔成交成交量价
     */
    List<UnitDealVO> unitDealVOS;

    /**
     * 出清曲线起点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> start;

    /**
     * 出清曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> end;




}
