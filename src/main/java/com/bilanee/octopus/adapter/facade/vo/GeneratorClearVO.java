package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratorClearVO {

    /**
     * 日前节点电价曲线
     */
    List<Double> daPrice;

    /**
     * 火电机组的日前最小成交段，当不是火电机组时，这个字段为空
     */
    List<ClearedVO> daMinClears;

    /**
     * 日前各段中标量，1段~5段中标量，根据内层数组长度判断含有几段中标量，最多为5段
     */
    List<List<ClearedVO>> daClearedSections;


    /**
     * 火电机组的实时最小成交段，当不是火电机组时，这个字段为空
     */
    List<ClearedVO> rtMinClears;

    /**
     * 实时节点电价曲线
     */
    List<Double> rtPrice;


    /**
     * 实时各段中标量，1段~5段中标量，根据内层数组长度判断含有几段中标量，最多为5段
     */
    List<List<ClearedVO>> rtClearedSections;

}
