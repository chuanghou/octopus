package com.bilanee.octopus.basic;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidVO {

    Double price;
    Double quantity;

}
