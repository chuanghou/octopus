package com.bilanee.octopus.adapter.tunnel;

import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.Section;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterClearance {

    StageId stageId;
    TimeFrame timeFrame;
    Double sellDeclaredQuantity;
    Double buyDeclaredQuantity;
    Double dealQuantity;
    Double dealPrice;
    GridLimit transLimit;
    List<Section> buySections;
    Point<Double> buyTerminus;
    List<Section> sellSections;
    Point<Double> sellTerminus;
    Double marketQuantity;
    Double nonMarketQuantity;

}
