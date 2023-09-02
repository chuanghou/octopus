package com.bilanee.octopus.adapter;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidQuery {

    Long compId;
    Long unitId;
    Integer roundId;
    Province province;
    TradeStage tradeStage;
    Direction direction;
    BidStatus bidStatus;

}
