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
public class RollDealVO {

    /**
     * 成交量
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;

    /**
     * 成交价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;

    /**
     * 成交时间
     */
    Long timeStamp;

}
