package com.bilanee.octopus.basic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CentralizedBidVO {

    Long unitId;
    List<BidVO> peakBidVOs;
    List<BidVO> flatBidVOs;
    List<BidVO> valleyBidVOs;
}
