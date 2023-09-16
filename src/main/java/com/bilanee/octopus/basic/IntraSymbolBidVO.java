package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.infrastructure.entity.Ask;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraSymbolBidVO {


    /**
     * 运行阶段
     */
    TimeFrame timeFrame;

    /**
     * 省份
     */
    Province province;

    /**
     * 分省分时段最新成交价
     */
    Double latestPrice;

    /**
     * 分时段交易盘 买一至买五
     */
    List<Ask> buyAsks;

    /**
     * 分时段交易盘, 卖一至卖五
     */
    List<Ask> sellAsks;

    /**
     * 分时段交易盘, 不同价格区间交易量
     */
    List<Double> buySections;

    /**
     * 分时段交易盘, 不同价格区间交易量
     */
    List<Double> sellSections;


    List<UnitIntraBidVO> unitIntraBidVOs;



}