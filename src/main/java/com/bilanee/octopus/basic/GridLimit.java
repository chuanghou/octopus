package com.bilanee.octopus.basic;

import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.ErrorEnumsBase;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GridLimit {

    Double high;
    Double low;

    public void check(Double price) {
        if (price > high || price < low) {
            throw new BizEx(ErrorEnumsBase.PARAM_FORMAT_WRONG.message("价格不满足高低门槛要求"));
        }
    }

}
