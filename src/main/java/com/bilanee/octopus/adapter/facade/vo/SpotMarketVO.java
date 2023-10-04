package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotMarketVO {

    List<UnitVO> unitVOs;

    List<SpotMarketEntityVO> daEntityVOs;

    List<SpotMarketEntityVO> rtEntityVOs;

}
