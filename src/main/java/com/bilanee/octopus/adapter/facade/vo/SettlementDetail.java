package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.infrastructure.entity.LoadsAssignedToTraderResults;
import com.bilanee.octopus.infrastructure.entity.UnitsAssignedToTraderResults;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettlementDetail {

    List<UnitsAssignedToTraderResults> generatorDetails;
    List<LoadsAssignedToTraderResults> loadDetails;

}
