package com.bilanee.octopus.adapter.facade;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 单元名字
     */
    String unitName;

}
