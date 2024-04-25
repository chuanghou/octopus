package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

  @TableId(type = IdType.INPUT)
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
  Double hotStartupTime;
  Double warmStartupTime;
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
