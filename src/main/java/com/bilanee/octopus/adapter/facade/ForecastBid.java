package com.bilanee.octopus.adapter.facade;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForecastBid {

    /**
     * 机组短期申报预测值相对于短期实际预测值的百分比
     */
    Double ratio;

    /**
     * 对应时刻起点
     */
    Double start;

    /**
     * 对应时刻终点
     */
    Double end;

    /**
     * 短期实际预测值
     */
    List<Double> forecast;


}
