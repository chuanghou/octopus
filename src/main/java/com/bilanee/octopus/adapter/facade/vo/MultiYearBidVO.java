package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.adapter.facade.po.MultiYearBid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MultiYearBidVO {

    /**
     * 分省分电源电网申报需求
     */
    Double require;

    /**
     * 单元报价信息
     */
    List<MultiYearBid> multiYearBids;
}
