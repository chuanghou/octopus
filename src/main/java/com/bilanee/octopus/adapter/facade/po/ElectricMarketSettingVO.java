package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.GridLimit;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

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
     * 省间交易的政府定价
     */
    Double regulatedInterprovTransmissionPrice;


    /**
     * 保障性用户电价
     */
    Double regulatedUserTariff;

    /**
     * 容量电价（元/MWyear）
     */
    Double capacityPrice;

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


    /**
     * 零售电价基于现货价格预测的倍数
     */
    Double retailPriceForecastMultiple;

    /**
     * 使能单点登录限制
     */
    Boolean singleLoginLimit;

    /**
     * 发电侧中长期持仓考核要求（%）
     */
    Double minForwardUnitPosition;
    /**
     * 用户侧中长期持仓考核要求（%）
     */
    Double minForwardLoadPosition;
    /**
     * 各设备省间可交易额度相对于按容量均分的倍数
     */
    Double maxForwardClearedMwMultiple;

    /**
     * 风力新能源价格上限
     */
    Double windSpecificPriceCap;


    /**
     * 光伏新能源价格上限
     */
    Double solarSpecificPriceCap;

    /**
     * 新能源专场交易电网申报需求占新能源预测上网电量百分比
     */
    RenewableSpecialTransactionDemandPercentage renewableSpecialTransactionDemandPercentage;


}

