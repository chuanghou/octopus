package com.bilanee.octopus.infrastructure.entity;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ask {

    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;

}
