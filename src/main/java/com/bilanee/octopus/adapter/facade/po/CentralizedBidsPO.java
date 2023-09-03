package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.TimeFrame;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Valids;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CentralizedBidsPO {

    /**
     * 系统阶段id
     */
    @NotBlank
    String stageId;

    /**
     * 集中竞价委托
     */
    @NotEmpty @Size(min = 9, max = 9, message = "数量不满足要求") @Valids
    List<BidPO> bidPOs;


    @AfterValidation
    public void afterValidation() {
        long count = bidPOs.stream().map(BidPO::getUnitId).distinct().count();
        BizEx.trueThrow(count != 1L, ErrorEnums.PARAM_FORMAT_WRONG);
        count = bidPOs.stream().map(BidPO::getDirection).distinct().count();
        BizEx.trueThrow(count != 1L, ErrorEnums.PARAM_FORMAT_WRONG);
        ListMultimap<TimeFrame, BidPO> grouped = bidPOs.stream().collect(Collect.listMultiMap(BidPO::getTimeFrame));
        BizEx.trueThrow(grouped.size() != 3, ErrorEnums.PARAM_FORMAT_WRONG);
        grouped.asMap().values().forEach(vs -> BizEx.trueThrow(vs.size() != 3, ErrorEnums.PARAM_FORMAT_WRONG));
    }

}
