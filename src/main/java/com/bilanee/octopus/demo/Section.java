package com.bilanee.octopus.demo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Section {

    Long unitId;
    Double leftX;
    Double rightX;
    Double y;

}
