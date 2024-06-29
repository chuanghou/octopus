package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RollBidPO {

    /**
     * 系统阶段id
     */
    @NotBlank
    String stageId;

    /**
     * 省间量价报单
     */
    @Valid
    BidPO bidPO;

}
