package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.Operation;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraBidContainer {

    Operation operation;
    Bid declareBid;
    Long cancelBidId;
    Direction cancelBidDirection;

}
