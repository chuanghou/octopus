package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.adapter.facade.Segment;
import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Valids;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraDaBidPO {

    @NotNull @Positive
    Long unitId;

    /**
     * 分段申报值
     */
    @Size(min = 5, max = 5, message = "应该是5个值") @Valids
    List<Segment> segments;

    /**
     * 短期实际预测值
     */
    @Size(min = 24, max = 24, message = "应该是24个值")
    List<Double> declares;

    @AfterValidation
    public void afterValidation() {
        if (Collect.isNotEmpty(segments)) {
            segments.forEach(s -> BizEx.trueThrow(s.getStart() >= s.getEnd(),
                    ErrorEnums.PARAM_FORMAT_WRONG.message("报价段起点必须小于终点")));
            for (int i = 0; i < segments.size() - 1; i++) {
                boolean equals = segments.get(i).getEnd().equals(segments.get(i + 1).getStart());
                BizEx.falseThrow(equals, ErrorEnums.PARAM_FORMAT_WRONG.message("报价段必须连续"));
                boolean b = segments.get(i).getPrice() <= segments.get(i + 1).getPrice();
                BizEx.falseThrow(b, ErrorEnums.PARAM_FORMAT_WRONG.message("报价必须为不递减序列"));
            }
        }
        if (Collect.isNotEmpty(declares)) {
            declares.forEach(d -> BizEx.trueThrow(d < 0, ErrorEnums.PARAM_FORMAT_WRONG.message("报价量大于等于0")));
        }

    }


}
