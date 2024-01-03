package com.bilanee.octopus.basic;

import com.bilanee.octopus.DoubleSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.ErrorEnumsBase;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GridLimit {

    @NotNull(message = "最大值不为空")
    @JsonSerialize(using = DoubleSerialize.class)
    Double high;

    @NotNull(message = "最小值不为空")
    @JsonSerialize(using = DoubleSerialize.class)
    Double low;

    public void check(Double price) {
        if (price > high || price < low) {
            throw new BizEx(ErrorEnumsBase.PARAM_FORMAT_WRONG.message("价格不满足高低门槛要求"));
        }
    }

}
