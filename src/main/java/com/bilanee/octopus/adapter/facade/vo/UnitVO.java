package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.basic.enums.GeneratorType;
import com.bilanee.octopus.basic.enums.UnitType;
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

    /**
     * 分配单元信息
     */
    MetaUnit metaUnit;

}
