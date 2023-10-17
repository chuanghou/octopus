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
public class NodalPriceVoltage {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Double daLmp;
  Double rtLmp;
  Double daVoltMag;
  Double daVoltAng;
  Integer nodeId;

}
