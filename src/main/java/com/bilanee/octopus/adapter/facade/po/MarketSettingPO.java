package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MarketSettingPO {

    /**
     * 机组申报上下限制
     */
    @NotNull(message = "机组申报上下限不可为空") @Valid
    GridLimitPO generatorPriceLimit;

    /**
     * 负荷申报上下限制
     */
    @NotNull(message = "负荷申报上下限不可为空") @Valid
    GridLimitPO loadPriceLimit;

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
     * 煤价变动倍数
     */
    @NotNull(message = "煤价变动倍数不可为空")
    Double coalPriceMultiple;


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


}
