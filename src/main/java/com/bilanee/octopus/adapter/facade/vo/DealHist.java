package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DealHist {

    @JsonSerialize(using = DoubleSerialize.class)
    Double left;
    @JsonSerialize(using = DoubleSerialize.class)
    Double right;
    @JsonSerialize(using = DoubleSerialize.class)
    Double value;

}
