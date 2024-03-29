package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartUpCostVO {

    Double coldStartupCost;
    Double warmStartupCost;
    Double hotStartupCost;

}
