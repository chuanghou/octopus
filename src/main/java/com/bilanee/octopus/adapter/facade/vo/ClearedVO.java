package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@lombok.Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClearedVO {

    /**
     * 成本
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double cost;

    /**
     * 中标量
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;

    /**
     * 中标价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;


    /**
     * 申报量
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double declared;

}
