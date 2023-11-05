package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("unit_basic")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratorBasic {

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
  @TableField("t_to_MWh")
  Double tToMWh;
  Double maxAgc;
  Double minAgc;
  Double maxSpinRes;
  Double unitResFactor;
  Double maxP;
  Double minP;
  Double maxQ;
  Double minQ;
  Integer numStartupCurvePrds;
  Integer numShutdownCurvePrds;
  Integer nodeId;
  Integer plantId;
  Integer unitgroupId;

}
