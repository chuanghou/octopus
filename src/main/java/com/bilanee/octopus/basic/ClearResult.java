package com.bilanee.octopus.basic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClearResult {
    Point<Integer> intersection;

    @Builder.Default
    List<PointLine> buyPointLines = new ArrayList<>();
    @Builder.Default
    List<PointLine> sellPointLines = new ArrayList<>();
    @Builder.Default
    List<Deal> deals = new ArrayList<>();

}
