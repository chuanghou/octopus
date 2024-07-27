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


  /**
   * 月度受电预测上限 （用于堆叠图）（MW）	decimal			新增，=max(月度需求缺口*110%,省间年度市场+计划的成交量)
   */
  Double maxMonthlyReceivingForecastMw;

  /**
   * 月度受电预测下限（用于堆叠图）（MW）	decimal			新增，=max(月度受电目标下限,日前受电目标,省间年度市场+计划的成交量)
   */
  Double minMonthlyReceivingForecastMw;

  Double maxAnnualReceivingMw;
  Double maxMonthlyReceivingMw;
  Double minAnnualReceivingMw;
  Double minMonthlyReceivingMw;
  Double intraprovincialAnnualTielinePower;
  Double intraprovincialMonthlyTielinePower;
  Double daReceivingTarget;
  Double daReceivingForecastMw;

}
