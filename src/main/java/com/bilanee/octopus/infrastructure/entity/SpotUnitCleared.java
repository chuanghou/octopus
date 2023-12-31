package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("intraprovincial_spot_unit_cleared")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotUnitCleared {

  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Double preclearClearedMw;
  Double daClearedMw;
  Double rucClearedMw;
  Double rtClearedMw;
  Double rtVariableCost;
  Boolean onoffStatus;
  Boolean isStartup;
  Boolean isColdStartup;
  Boolean isWarmStartup;
  Boolean isHotStartup;
  Boolean isShutdown;
  Integer unitId;

}
