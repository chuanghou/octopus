package com.bilanee.octopus.adapter.facade.vo;

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

    Double cost;
    Double quantity;
    Double price;


    public ClearedVO point2() {
        return ClearedVO.builder()
                .cost(Double.parseDouble(String.format("%.2f", cost)))
                .quantity(Double.parseDouble(String.format("%.2f", quantity)))
                .price(Double.parseDouble(String.format("%.2f", price)))
                .build();
    }
}
