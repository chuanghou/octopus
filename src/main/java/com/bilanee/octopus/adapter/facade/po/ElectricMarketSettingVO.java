package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.GridLimit;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElectricMarketSettingVO {

    /**
     * 机组申报上下限制
     */
    GridLimit generatorPriceLimit;

    /**
     * 负荷申报上下限制
     */
    GridLimit loadPriceLimit;

    /**
     * 基数电量电价
     */
    Double regulatedProducerPrice;


    /**
     * 输配电价
     */
    Double transmissionAndDistributionTariff;


    /**
     * 省间交易的政府定价
     */
    Double regulatedInterprovTransmissionPrice;


    /**
     * 保障性用户电价
     */
    Double regulatedUserTariff;


    /**
     * 煤价变动倍数
     */
    Double coalPriceMultiple;


    /**
     * 中长期机组持仓量上限比例
     */
    Double maxForwardUnitPositionInterest;


    /**
     * 中长期负荷持仓量上限比例
     */
    Double maxForwardLoadPositionInterest;


    /**
     * 机组合约市场报价段数
     */
    Integer forwardNumOfferSegs;

    /**
     * 负荷合约市场报价段数
     */
    Integer forwardNumBidSegs;

    /**
     * 机组现货报价段数
     */
    Integer spotNumOfferSegs;

    /**
     * 负荷现货报价段数
     */
    Integer spotNumBidSegs;

    /**
     * 省间交易组织形式
     */
    String interprovTradingMode;

    /**
     * 省间交易出清形式
     */
    String interprovClearingMode;


}

