package com.bilanee.octopus.demo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CentralizedClearance {

    Double sellDeclaredQuantity;
    Double buyDeclaredQuantity;
    Double dealQuantity;
    Double dealPrice;

    List<UnitVO> unitVOs;

    List<Section> buySections;

    List<Section> sellSections;

}
