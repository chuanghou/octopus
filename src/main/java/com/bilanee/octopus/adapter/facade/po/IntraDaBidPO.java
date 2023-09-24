package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.adapter.facade.ForecastBid;
import com.bilanee.octopus.adapter.facade.SegmentBid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraDaBidPO {

    @NotNull
    Long unitId;

    @Valid
    SegmentBid segmentBid;

    @Valid
    ForecastBid forecastBid;

}
