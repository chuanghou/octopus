package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.PointSerialize;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.Section;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
     * 市场供需曲线：成本曲线
     */
    List<Section> costSections;

    /**
     * 市场供需曲线：成本曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> costTerminus;


    /**
     * 市场供需曲线：供给曲线
     */
    List<Section> supplySections;

    /**
     * 市场供需曲线：供给曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> supplyTerminus;

    /**
     * 市场供需曲线：需求曲线
     */
    List<Section> demandSections;

    /**
     * 市场供需曲线：需求曲线终点
     */
    @JsonSerialize(using = PointSerialize.class)
    Point<Double> demandTerminus;



}
