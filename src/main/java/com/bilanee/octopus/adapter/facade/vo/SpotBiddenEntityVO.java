package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoublesSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotBiddenEntityVO {

    /**
     * 省内负荷-省间受电
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> intraLoadMinusInterIn;

    /**
     * 省内负荷+省间外送
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> intraLoadPlusInterOut;

    /**
     * 新能源未中标
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> renewableNotBidden;

    /**
     * 火电未中标
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> classicNotBidden;

    /**
     * 火电中标
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> classicBidden;

    /**
     * 新能源中标
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> renewableBidden;

    /**
     * 0-200元/Mwh 200-400元/Mwh 400-600元/Mwh 600-800元/Mwh 800元/Mwh以上
     */
    List<List<Double>> priceStatistics;
}
