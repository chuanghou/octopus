package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Segment {

    /**
     * 起始出力
     */
    @NotNull @PositiveOrZero
    @JsonSerialize(using = DoubleSerialize.class)
    Double start;

    /**
     * 终止出力
     */
    @NotNull @PositiveOrZero
    @JsonSerialize(using = DoubleSerialize.class)
    Double end;

    /**
     * 申报价格
     */
    @NotNull @PositiveOrZero
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;

}
