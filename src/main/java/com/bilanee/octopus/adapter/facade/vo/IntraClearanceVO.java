package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.adapter.CustomerDoubleSerialize;
import com.bilanee.octopus.basic.enums.Province;
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
public class IntraClearanceVO {

    /**
     * 省份
     */
    Province province;

    /**
     * 运行时段
     */
    TimeFrame timeFrame;

    /**
     *  成交均价
     */
    Double averageDealPrice;

    /**
     *  最高成交价格
     */
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double maxDealPrice;

    /**
     *  最低成交价格
     */
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double minDealPrice;



    /**
     *  总成交电力
     */
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double totalDealQuantity;


    /**
     *  卖方剩余未成交电力
     */
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double sellTotalTransit;


    /**
     *  买方剩余未成交电力
     */
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double buyTotalTransit;

    /**
     *  成交直方图
     */
    List<DealHist> dealHists;

    /**
     * 市场供需曲线/分设备成交量价/分设备分笔成交量价：单元情况
     */
    List<UnitVO> unitVOs;


    /**
     * 分设备平均成交量价/分设备分笔成交成交量价
     */
    List<UnitDealVO> unitDealVOS;




}
