package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceVO {

    Direction direction;
    Double balance;

}
