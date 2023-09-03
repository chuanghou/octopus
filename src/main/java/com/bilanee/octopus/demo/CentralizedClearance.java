package com.bilanee.octopus.demo;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
