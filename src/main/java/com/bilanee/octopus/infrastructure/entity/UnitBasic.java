package com.bilanee.octopus.infrastructure.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitBasic {

  Integer unitId;
  String unitName;
  Integer prov;
  Integer type;
  Double ratedVolt;
  Double ratedCapacity;
  Double inplantUseFactor;
  Double minOffDuration;
  Double minOnDuration;
  Double rampUpRate;
  Double rampDnRate;
  Double coldStartupCost;
  Double warmStartupCost;
  Double hotStartupCost;
  Double hotStartupTime;
  Double warmStartupTime;
  Double tToMWh;
  Double maxAgc;
  Double minAgc;
  Double maxSpinRes;
  Double unitResFactor;
  Double maxP;
  Double minP;
  Double maxQ;
  Double minQ;
  Double startupCurve1;
  Double startupCurve2;
  Double startupCurve3;
  Double startupCurve4;
  Double startupCurve5;
  Double startupCurve6;
  Double shutdownCurve1;
  Double shutdownCurve2;
  Double shutdownCurve3;
  Double shutdownCurve4;
  Double shutdownCurve5;
  Double shutdownCurve6;
  Integer numStartupCurvePrds;
  Integer numShutdownCurvePrds;
  Integer nodeId;
  Integer plantId;
  Integer unitgroupId;

}
