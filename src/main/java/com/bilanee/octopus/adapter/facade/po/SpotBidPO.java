package com.bilanee.octopus.adapter.facade.po;

import com.stellariver.milky.common.base.Valids;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
    @NotNull
    Long unitId;

    /**
     * 分段量价
     */
    @NotEmpty @Valids
    List<InstantSpotBidPO> instantSpotBidPOs;

}
