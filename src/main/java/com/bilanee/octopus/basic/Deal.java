package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Deal {

    Long id;
    TimeFrame timeFrame;
    Long buyUnitId;
    Long sellUnitId;
    Double quantity;
    Double price;
    Long timeStamp;

}
