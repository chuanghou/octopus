package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RealtimeBidPO {

    /**
     * 系统阶段id
     */
    String stageId;

    /**
     * 省内竞赛的量价报单
     */
    BidPO bidPO;

}
