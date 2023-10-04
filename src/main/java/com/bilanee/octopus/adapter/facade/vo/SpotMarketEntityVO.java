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
     * 时刻，0~23
     */
    Integer instant;

    /**
     * 市场供需曲线：需求曲线
     */
    List<Section> buySections;

    /**
     * 市场供需曲线：需求曲线终点
     */
    Point<Double> buyTerminus;

    /**
     * 市场供需曲线：供给曲线
     */
    List<Section> sellSections;

    /**
     * 市场供需曲线：供给曲线终点
     */
    Point<Double> sellTerminus;

}
