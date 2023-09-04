package com.bilanee.octopus.adapter.tunnel;

import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.TimeFrame;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WriteBackBO {

    StageId stageId;
    TimeFrame timeFrame;
    Double marketQuantity;
    Double nonMarketQuantity;

}
