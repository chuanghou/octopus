package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "thermal_unit_operating_cost",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ThermalCostDO {

  @TableId
  Long id;
  String dt;
  Integer prd;
  Integer spotCostId;
  Double spotCostMarginalCost;
  Double spotCostMw;
  Integer unitId;

}
