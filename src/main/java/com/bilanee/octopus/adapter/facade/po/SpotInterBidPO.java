package com.bilanee.octopus.adapter.facade.po;


import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.tool.common.Kit;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotInterBidPO {


    @NotNull @Positive
    Long unitId;

    @NotEmpty @Size(min = 3, max = 3)
    List<SpotBidPO> spotBidPOs;

}
