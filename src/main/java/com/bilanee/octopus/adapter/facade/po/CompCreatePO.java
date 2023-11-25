package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompCreatePO {

    /**
     * 竞赛开始时间戳，可以为空，为空的时候，默认5分钟之后开始
     */
    @Positive
    Long startTimeStamp;

}
