package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraDealVO {

    /**
     * 成交量
     */
    Double quantity;

    /**
     * 成交价格
     */
    Double price;

    /**
     * 成交时间
     */
    Long timeStamp;

}
