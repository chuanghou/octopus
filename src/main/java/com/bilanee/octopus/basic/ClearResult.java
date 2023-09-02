package com.bilanee.octopus.basic;

import com.stellariver.milky.demo.basic.PointLine;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClearResult {
    List<PointLine> buyPointLines;
    List<PointLine> sellPointLines;
    Pair<Double, Double> interPoint;
    List<Deal> deals;
}
