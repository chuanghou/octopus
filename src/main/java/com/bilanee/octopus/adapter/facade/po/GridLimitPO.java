package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GridLimitPO {

    @NotNull(message = "最高价不可为空")
    Double high;

    @NotNull(message = "最低价不可为空")
    Double low;


}
