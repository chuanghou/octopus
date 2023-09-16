package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.UnitType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitInterBidVO {

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
     * 送电省/受电省
     */
    Province province;

    /**
     * 单元类型
     */
    UnitType unitType;


    /**
     * 峰平谷三段报价报量
     */
    List<InterBidVO> interBidVOS;

}
