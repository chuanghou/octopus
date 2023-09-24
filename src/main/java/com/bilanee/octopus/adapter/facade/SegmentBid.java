package com.bilanee.octopus.adapter.facade;

import java.util.List;

import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Valids;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SegmentBid {

    @NotEmpty @Size(min = 5, max = 5) @Valids
    List<Segment> segments;


    @AfterValidation
    public void afterValidation() {
        segments.forEach(s -> BizEx.trueThrow(s.getStart() >= s.getEnd(),
                ErrorEnums.PARAM_FORMAT_WRONG.message("报价段起点必须小于终点")));
        for (int i = 0; i < segments.size() - 1; i++) {
            boolean equals = segments.get(i).getEnd().equals(segments.get(i + 1).getStart());
            BizEx.falseThrow(equals, ErrorEnums.PARAM_FORMAT_WRONG.message("报价段必须连续"));
        }
    }

}
