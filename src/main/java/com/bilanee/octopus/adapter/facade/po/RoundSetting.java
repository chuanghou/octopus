package com.bilanee.octopus.adapter.facade.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoundSetting {

    /**
     * 送电省负荷峰谷差 大（true）小（false）
     */
    @NotNull
    Boolean transferDiffer;

    /**
     * 送电省负荷峰值 高（true）低（false）
     */
    @NotNull
    Boolean transferLoadPeak;

    /**
     * 受电省负荷峰谷差 大（true）小（false）
     */
    @NotNull
    Boolean receiverDiffer;

    /**
     * 受电省负荷峰值 高（true）低（false）
     */
    @NotNull
    Boolean receiverLoadPeak;

    /**
     * 输配电价
     */
    @NotNull(message = "输配电价不可为空")
    Double transmissionAndDistributionTariff;

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

}
