package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.GridLimit;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElectricMarketSetting {

    /**
     * 机组申报上下限制
     */
    @NotNull(message = "机组申报上下限不可为空") @Valid
    GridLimit generatorPriceLimit;

    /**
     * 负荷申报上下限制
     */
    @NotNull(message = "负荷申报上下限不可为空") @Valid
    GridLimit loadPriceLimit;

    /**
     * 基数电量电价
     */
    @NotNull(message = "基数电量电价不可为空")
    Double regulatedProducerPrice;





    /**
     * 省间交易的政府定价
     */
    @NotNull(message = "省间交易电价不可为空")
    Double regulatedInterprovTransmissionPrice;


    /**
     * 保障性用户电价
     */
    @NotNull(message = "保障性用户电价不可为空")
    Double regulatedUserTariff;


    /**
     * 容量电价（元/MWyear）
     */
    @NotNull(message = "容量电价不可为空")
    Double capacityPrice;

    /**
     * 中长期机组持仓量上限比例
     */
    @NotNull(message = "中长期机组持仓量上限比例不可为空")
    Double maxForwardUnitPositionInterest;


    /**
     * 中长期负荷持仓量上限比例
     */
    @NotNull(message = "中长期负荷持仓量上限比例不可为空")
    Double maxForwardLoadPositionInterest;

    /**
     * 零售电价基于现货价格预测的倍数
     */
    @NotNull(message = "零售电价基于现货价格预测的倍数不可为空")
    @Min(value = 1, message = "最小为1") @Max(value = 6, message = "最大为6")
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
