package com.bilanee.octopus.basic;

import com.bilanee.octopus.DoubleSerialize;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;
    Long timeStamp;

}
