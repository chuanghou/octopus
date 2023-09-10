package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraMarketHistoryDO {

    @TableId(type = IdType.AUTO)
    Long id;
    String stageId;
    Province province;
    TimeFrame timeFrame;
    Double buyQuantity;
    Double sellQuantity;
    Double latestPrice;
    Long timeStamp;

}
