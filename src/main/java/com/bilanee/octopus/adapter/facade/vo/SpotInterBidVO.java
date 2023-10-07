package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.GridLimit;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.tool.common.Kit;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotInterBidVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 单元名称
     */
    String unitName;

    /**
     * 分时刻量价
     */
    List<InstantSpotBidVO> instantSpotBidVOs;

    /**
     * 价格限制
     */
    GridLimit priceLimit;

}
