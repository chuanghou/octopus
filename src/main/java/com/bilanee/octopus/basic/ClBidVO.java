package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClBidVO {

    TimeFrame timeFrame;
    Double capacity;
    List<BalanceVO> balanceVOs;
    List<BidVO> bidVOs;

}
