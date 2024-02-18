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
@FieldDefaults(level = AccessLevel.PRIVATE)
@TableName(value = "intraprovincial_spot_thermal_start_offer", autoResultMap = true)
public class IntraOffer {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  Integer unitId;
  String dt;
  Integer prd;
  Double coldStartupOffer;
  Double warmStartupOffer;
  Double hotStartupOffer;
  Double unLoadOffer;

}
