package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstantSpotBidVO {

    /**
     * 时刻
     */
    Integer instant;

    /**
     * 发电能力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double maxCapacity;

    /**
     * 预出清中标量
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double preCleared;

    /**
     * 分段量价
     */
    List<InterSpotBid> interSpotBids;


}
