package com.bilanee.octopus.basic;

import com.bilanee.octopus.adapter.CustomerDoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double lx;

    /**
     * 线段右x轴坐标
     */
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double rx;

    /**
     * 线段y轴坐标
     */
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double y;

}
