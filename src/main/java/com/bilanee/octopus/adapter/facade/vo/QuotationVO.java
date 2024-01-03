package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuotationVO {

    /**
     * 时间
     */
    Long timeStamp;

    /**
     * 成交价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;

    /**
     * 买单总量
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double buyQuantity;


    /**
     * 卖单总量
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double sellQuantity;

}
