package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.adapter.CustomerDoubleSerialize;
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

    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double cost;
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double quantity;
    @JsonSerialize(using = CustomerDoubleSerialize.class)
    Double price;

}
