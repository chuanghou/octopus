package com.bilanee.octopus.basic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterUnitVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 价格约束
     */
    GridLimit priceLimit;

    /**
     * 单元名
     */
    String unitName;

    /**
     * 峰平谷三段报价报量
     */
    List<InterBidVO> interBidVOS;

}
