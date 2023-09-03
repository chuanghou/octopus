package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.Direction;
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
public class PointLine {

    Long bidId;
    Long unitId;
    Direction direction;
    Integer quantity;
    Integer price;

    Integer leftX;
    Integer rightX;
    Integer width;
    Integer y;
}
