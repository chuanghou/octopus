package com.bilanee.octopus.basic;

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
    Double quantity;

}
