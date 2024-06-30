package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
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
public class RollClearanceVO {

    /**
     * 省份
     */
    Province province;

    /**
     * 运行时段
     */
    Integer instant;

    /**
     *  成交均价
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double averageDealPrice;

    /**
     *  最高成交价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double maxDealPrice;

    /**
     *  最低成交价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double minDealPrice;



    /**
     *  总成交电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double totalDealQuantity;


    /**
     *  卖方剩余未成交电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double sellTotalTransit;


    /**
     *  买方剩余未成交电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
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
