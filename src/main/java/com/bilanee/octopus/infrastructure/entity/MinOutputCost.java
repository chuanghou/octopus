package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinOutputCost {

  @TableId(type = IdType.INPUT)
  Long id;
  String dt;
  Integer prd;
  Double spotCostMinoutput;
  Double shutdownCurveCost;
  Double startupCurveCost;
  Integer unitId;

}
