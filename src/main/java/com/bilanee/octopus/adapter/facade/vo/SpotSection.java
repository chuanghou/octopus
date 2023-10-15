package com.bilanee.octopus.adapter.facade.vo;

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
    Double lx;

    /**
     * 线段右x轴坐标
     */
    Double rx;

    /**
     * 线段y轴坐标
     */
    Double y;

}
