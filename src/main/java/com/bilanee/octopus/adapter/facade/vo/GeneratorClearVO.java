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
     * 日前各段中标量，最小技术出力/开停机过程出力，1段~5段中标量，根据内层数组长度判断含有几段中标量，最多为6段，有可能最小技术出力都没有
     */
    List<List<Double>> daClearedSections;


    /**
     * 实时节点电价曲线
     */
    List<Double> rtPrice;


    /**
     * 实时各段中标量，最小技术出力/开停机过程出力，1段~5段中标量，根据内层数组长度判断含有几段中标量，最多为6段，有可能最小技术出力都没有
     */
    List<List<Double>> rtClearedSections;

}
