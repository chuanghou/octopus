package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * 中标电源结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotBiddenVO {

    /**
     * 日前
     */
    SpotBiddenEntityVO daSpotBiddenEntityVO;

    /**
     * 实时
     */
    SpotBiddenEntityVO rtSpotBiddenEntityVO;
}
