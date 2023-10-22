package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterSpotBid {

    /**
     * 数量
     */
    @NotNull @Positive
    Double quantity;

    /**
     * 价格
     */
    @NotNull @Positive
    Double price;


}
