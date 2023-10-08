package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.adapter.facade.vo.InterSpotBid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstantSpotBidPO {

    /**
     * 时刻
     */
    Integer instant;

    /**
     * 分段量价
     */
    List<InterSpotBid> interSpotBids;


}
