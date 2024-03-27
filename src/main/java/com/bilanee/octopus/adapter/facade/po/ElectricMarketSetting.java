package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.GridLimit;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.ErrorEnumsBase;
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
     * 输配电价
     */
    @NotNull(message = "输配电价不可为空")
    Double transmissionAndDistributionTariff;


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
     * 燃煤价格年度预测（元/t）
     */
    @NotNull(message = "燃煤价格年度预测不可为空")
    Double annualCoalPrice;

    /**
     * 燃煤价格月度预测（元/t）
     */
    @NotNull(message = "燃煤价格月度预测不可为空")
    Double monthlyCoalPrice;

    /**
     * 燃煤价格日前预测（元/t）
     */
    @NotNull(message = "燃煤价格日前预测不可为空")
    Double daCoalPrice;

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


}
