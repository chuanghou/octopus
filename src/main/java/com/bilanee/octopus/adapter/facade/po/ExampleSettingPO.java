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
public class ExampleSettingPO {


    /**
     * 送电省负荷峰谷差 大（true）小（false）
     */
    @NotNull
    Boolean transferDiffer;

    /**
     * 受电省负荷峰谷差 大（true）小（false）
     */
    @NotNull
    Boolean receiverDiffer;

    /**
     * 送电省负荷峰值 高（true）低（false）
     */
    @NotNull
    Boolean transferLoadPeak;

    /**
     * 受电省负荷峰值 高（true）低（false）
     */
    @NotNull
    Boolean receiverLoadPeak;


    /**
     * 负荷年度预测百分比误差
      */
    @NotNull @Valid
    ForecastError loadAnnualMaxForecastErr;

    /**
     * 负荷月度预测百分比误差
     */
    @NotNull @Valid
    ForecastError loadMonthlyMaxForecastErr;

    /**
     * 负荷日前预测百分比误差
     */
    @NotNull @Valid
    ForecastError loadDaMaxForecastErr;

    /**
     * 新能源年度预测百分比误差
     */
    @NotNull @Valid
    ForecastError renewableAnnualMaxForecastErr;

    /**
     * 新能源月度预测百分比误差
     */
    @NotNull @Valid
    ForecastError renewableMonthlyMaxForecastErr;

    /**
     * 新能源日前预测百分比误差
     */
    @NotNull @Valid
    ForecastError renewableDaMaxForecastErr;

}
