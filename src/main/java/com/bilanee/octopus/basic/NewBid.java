package com.bilanee.octopus.basic;

import com.bilanee.octopus.common.enums.Province;
import com.bilanee.octopus.common.enums.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewBid {

    Long bidId;
    Long unitId;
    Province province;
    TimeFrame timeFrame;
    TradeStage tradeStage;
    Direction direction;
    Double quantity;
    Double price;
    Date date;

}
