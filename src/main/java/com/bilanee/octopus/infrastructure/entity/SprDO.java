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
@TableName(value = "system_parameter_release",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SprDO {

  @TableId(type = IdType.INPUT)
  Long id;
  String dt;
  Integer prd;
  Integer prov;
  Double minThermalMw;
  Double adjustableThermalMw;
  Double annualLoadForecast;
  Double monthlyLoadForecast;
  Double daLoadForecast;
  Double rtLoad;
  Double annualRenewableForecast;
  Double monthlyRenewableForecast;
  Double daRenewableForecast;
  Double rtRenewable;
  Double resUp;
  Double resDn;
  Double minThermalCapacity;
  Double coalPrice;
  Double renewableGovernmentSubsidy;

}
