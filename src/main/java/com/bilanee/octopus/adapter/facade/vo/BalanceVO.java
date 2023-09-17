package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.enums.Direction;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceVO {

    Direction direction;
    Double balance;

}
