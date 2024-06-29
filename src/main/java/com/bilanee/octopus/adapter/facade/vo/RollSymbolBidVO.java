package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.bilanee.octopus.basic.Volume;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.infrastructure.entity.Ask;
import com.bilanee.octopus.infrastructure.entity.StepRecord;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RollSymbolBidVO {

    /**
     * 省份
     */
    Province province;

    /**
     * 运行时刻点0~23
     */
    Integer instant;

    /**
     * 分省分时段最新成交价
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double latestPrice;

    /**
     * 卖委托最高价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double sellHighestPrice;

    /**
     * 卖委托最低价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double sellLowestPrice;

    /**
     * 买委托最高价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double buyHighestPrice;

    /**
     * 买委托最低价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double buyLowestPrice;

    /**
     * 分时段交易盘 买一至买五
     */
    List<Ask> buyAsks;

    /**
     * 分时段交易盘, 卖一至卖五
     */
    List<Ask> sellAsks;

    /**
     * 分时段交易盘, 不同价格区间交易量，买单
     */
    List<Volume> buyVolumes;

    /**
     * 分时段交易盘, 不同价格区间交易量，卖单
     */
    List<Volume> sellVolumes;

    /**
     * 报单区
     */
    List<UnitRollBidVO> unitRollBidVOs;

    /**
     * 实时成交曲线
     */
    List<QuotationVO> quotationVOs;

    /**
     * 阶段起始时间
     */
    StepRecord stepRecord;



}
