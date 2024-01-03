package com.bilanee.octopus.basic;

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
public class Volume {

    String label;
    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;

}
