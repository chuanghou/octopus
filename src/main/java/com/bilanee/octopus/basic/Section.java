package com.bilanee.octopus.basic;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Section {

    /**
     * 单元id
     */
    Long unitId;

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
