package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotSection {

    /**
     * 线段左x轴坐标
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double lx;

    /**
     * 线段右x轴坐标
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double rx;

    /**
     * 线段y轴坐标
     */
    Double y;

}
