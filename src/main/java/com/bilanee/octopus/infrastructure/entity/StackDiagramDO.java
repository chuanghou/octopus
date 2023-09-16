package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "tieline_power_band_for_stack_diagram", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StackDiagramDO {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Integer prov;
  Double maxAnnualReceivingMw;
  Double maxMonthlyReceivingMw;
  Double minAnnualReceivingMw;
  Double minMonthlyReceivingMw;
  Double annualReceivingForecastMw;
  Double monthlyReceivingForecastMw;
  Double intraprovincialAnnualTielinePower;
  Double intraprovincialMonthlyTielinePower;
  Double daReceivingTarget;
  Double daReceivingForecastMw;

}
