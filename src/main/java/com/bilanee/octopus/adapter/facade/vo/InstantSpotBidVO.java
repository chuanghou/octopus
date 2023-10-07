package com.bilanee.octopus.adapter.facade.vo;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
    Double maxCapacity;

    /**
     * 预出清中标量
     */
    Double preCleared;

    /**
     * 分段量价
     */
    List<SpotBidVO> spotBidVOs;


}