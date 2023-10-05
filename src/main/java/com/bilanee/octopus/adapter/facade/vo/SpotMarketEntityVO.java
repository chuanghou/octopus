package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.Section;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotMarketEntityVO {

    /**
     * 市场供需曲线：供给曲线
     */
    List<Section> supplySections;

    /**
     * 市场供需曲线：供给曲线终点
     */
    Point<Double> supplyTerminus;

    /**
     * 市场供需曲线：需求曲线
     */
    List<Section> demandSections;

    /**
     * 市场供需曲线：需求曲线终点
     */
    Point<Double> demandTerminus;



}
