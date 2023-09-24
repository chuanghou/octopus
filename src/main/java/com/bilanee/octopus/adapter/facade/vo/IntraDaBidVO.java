package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.adapter.facade.ForecastBid;
import com.bilanee.octopus.adapter.facade.SegmentBid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraDaBidVO {

    Long unitId;

    String unitName;

    @Nullable
    SegmentBid segmentBid;

    @Nullable
    ForecastBid forecastBid;

}
