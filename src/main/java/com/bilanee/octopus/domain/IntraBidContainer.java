package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.Bid;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraBidContainer {

    Bid declareBid;
    Long cancelBidId;
    Boolean close;

}
