package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraBidPO {

    /**
     * 系统阶段id
     */
    String stageId;

    /**
     * 省间量价报单
     */
    BidPO bidPO;

}
