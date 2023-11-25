package com.bilanee.octopus.infrastructure.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StepRecord {

    String stageId;

    Long startTimeStamp;

    Long endTimeStamp;

}
