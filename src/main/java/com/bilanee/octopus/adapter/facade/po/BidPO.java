package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

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
    @Positive @Digits(integer = 10, fraction = 0, message = "申报数量格式不满足要求")
    Double quantity;

    /**
     * 申报价格
     */
    @Digits(integer = 10, fraction = 0, message = "格式不满足要求")
    Double price;

    /**
     * 申报方向
     */
    Direction direction;

    /**
     * 运行阶段
     */
    @NotNull
    TimeFrame timeFrame;

}
