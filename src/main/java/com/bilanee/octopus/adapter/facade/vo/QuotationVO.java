package com.bilanee.octopus.adapter.facade.vo;

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
    Double price;

    /**
     * 买单总量
     */
    Double buyQuantity;


    /**
     * 卖单总量
     */
    Double sellQuantity;

}
