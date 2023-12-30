package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForecastError {

    /**
     * 误差
     */
    @NotNull @Range(max = 1L, message = "范围需要在0-1之前" )
    Double forecastError;

    /**
     * 送电省
     */
    @NotNull(message = "送电省对应参数不能是空")
    Double transfer;

    /**
     * 受电省
     */
    @NotNull(message = "受电省对应参数不能是空")
    Double receiver;


}
