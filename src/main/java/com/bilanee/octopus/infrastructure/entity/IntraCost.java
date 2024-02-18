package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "intraprovincial_spot_thermal_cost",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraCost {

  Long id;
  String dt;
  Integer prd;
  Double coldStartupCost;
  Double warmStartupCost;
  Double hotStartupCost;
  Double coalConsumptionQuadraticCoe;
  Double coalConsumptionPrimaryCoe;
  Double coalConsumptionConstantCoe;
  Double otherCost;
  Double costQuadraticCoe;
  Double costPrimaryCoe;
  Double costConstantCoe;
  Double coldStartupOfferCap;
  Double warmStartupOfferCap;
  Double hotStartupOfferCap;
  Double noLoadOfferCap;
  Integer unitId;

}
