package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RealtimeBidPO {

    /**
     * 系统阶段id
     */
    @NotBlank
    String stageId;

    /**
     * 省内竞赛的量价报单
     */
    @NotNull @Valid
    BidPO bidPO;

}
