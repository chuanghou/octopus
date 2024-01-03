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

    @JsonSerialize(using = DoubleSerialize.class)
    Double cost;
    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;

}
