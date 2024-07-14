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
public class ExampleSetting {

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
