package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotBidPO {

    /**
     * 阶段id
     */
    @NotBlank
    String stageId;

    /**
     * 单元id
     */
    @NotBlank
    Long unitId;

    /**
     * 分段量价
     */
    @NotEmpty
    List<InstantSpotBidPO> instantSpotBidPOs;

}
