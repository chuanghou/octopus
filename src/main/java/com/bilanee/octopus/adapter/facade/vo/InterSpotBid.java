package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterSpotBid {

    /**
     * 数量
     */
    @NotNull @PositiveOrZero
    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;

    /**
     * 价格
     */
    @NotNull @PositiveOrZero
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;


}
