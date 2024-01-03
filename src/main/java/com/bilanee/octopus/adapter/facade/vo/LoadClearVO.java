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
public class LoadClearVO {

    /**
     *  日前节点点电价曲线
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> daPrice;

    /**
     *  日前出清电量
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> daCleared;

    /**
     *  实时节点点电价曲线
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> rtPrice;

    /**
     *  实时出清电量
     */
    @JsonSerialize(using = DoublesSerialize.class)
    List<Double> rtCleared;

}
