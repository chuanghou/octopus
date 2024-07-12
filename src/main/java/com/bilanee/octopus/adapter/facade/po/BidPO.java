package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidPO {

    /**
     * 单元ID
     */
    @NotNull @Positive
    Long unitId;

    /**
     * 申报数量
     */
    @PositiveOrZero @Digits(integer = 10, fraction = 0, message = "申报数量格式不满足要求")
    Double quantity;

    /**
     * 申报价格
     */
    @NotNull(message = "价格不能为空")
    @Digits(integer = 10, fraction = 2, message = "格式不满足要求")
    Double price;

    /**
     * 申报方向
     */
    @NotNull(message = "方向不能为空")
    Direction direction;

    /**
     * 峰平谷
     */
    TimeFrame timeFrame;

    /**
     * 时刻，滚动撮合用
     */
    Integer instant;

    @AfterValidation
    public void afterValidation() {
        BizEx.trueThrow(timeFrame == null && instant == null, ErrorEnums.PARAM_IS_NULL.message("时刻/时段参数为空"));
    }

}
