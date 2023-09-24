package com.bilanee.octopus.adapter.facade;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Segment {

    /**
     * 起始出力
     */
    @NotNull @Positive
    Double start;

    /**
     * 终止出力
     */
    @NotNull @Positive
    Double end;

    /**
     * 申报价格
     */
    @NotNull @Positive
    Double price;

}
