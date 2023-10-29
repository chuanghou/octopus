package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.adapter.facade.Segment;
import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.enums.GeneratorType;
import com.bilanee.octopus.basic.enums.UnitType;
import com.stellariver.milky.common.base.Valids;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraDaBidVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 单元名称
     */
    String unitName;

    /**
     * 单元类型
     */
    UnitType unitType;

    /**
     * 机组类型
     */
    GeneratorType generatorType;


    Integer sourceId;

    /**
     * 分段式报价，当单元类型为机组时存在segments报价区
     */
    @Size(min = 5, max = 5) @Valids
    List<Segment> segments;


    /**
     * 火电机组的最小起始段
     */
    @Nullable
    Segment minSegment;

    /**
     * 机组成本曲线, 五段成本，画图用
     */
    List<Segment> costs;

    /**
     * 价格限制
     */
    GridLimit priceLimit;

    /**
     * 预测值，当机组类型为新能源类型或者单元类型为负载的时候，存在根据预测进行申报的界面
     */
    List<Double> forecasts;

    /**
     * 基于预测的申报值，当机组类型为新能源类型或者单元类型为负载的时候，存在根据预测进行申报的界面
     */
    List<Double> declares;


}
