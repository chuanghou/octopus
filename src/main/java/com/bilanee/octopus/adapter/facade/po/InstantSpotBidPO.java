package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.adapter.facade.vo.InterSpotBid;
import com.stellariver.milky.common.base.Valids;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstantSpotBidPO {

    /**
     * 时刻
     */
    @NotNull
    Integer instant;

    /**
     * 分段量价
     */
    @NotEmpty @Valids
    List<InterSpotBid> interSpotBids;


}
