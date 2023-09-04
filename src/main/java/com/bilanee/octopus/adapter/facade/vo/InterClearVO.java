package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.adapter.facade.UnitVO;
import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.demo.Section;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterClearVO {

    /**
     * 运行时段
     */
    TimeFrame timeFrame;

    /**
     *  市场成交情况：卖方总申报电力
     */
    Double sellDeclaredQuantity;

    /**
     *  市场成交情况：买方总申报电力
     */
    Double buyDeclaredQuantity;

    /**
     *  市场成交情况：成交电力
     */
    Double dealQuantity;


    /**
     *  市场成交情况：成交价格
     */
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
    Point<Double> buyTerminus;

    /**
     * 市场供需曲线：供给曲线
     */
    List<Section> sellSections;

    /**
     * 市场供需曲线：供给曲线终点
     */
    Point<Double> sellTerminus;


    /**
     * 分设备平均成交量价/分设备分笔成交成交量价
     */
    List<InterDealVO> interDealVOs;




}
