package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("thermal_unit_minoutput_and_startup_shutdown_cost")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinOutputCostDO {

  Long id;
  String dt;
  Integer prd;
  Double spotCostMinoutput;
  Double shutdownCurveCost;
  Double startupCurveCost;
  Integer unitId;

}
