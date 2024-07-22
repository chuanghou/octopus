package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("da_receiving_province_unmet_demand")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnmetDemand {

  Long id;
  String dt;
  Integer prd;
  Double daUnmetDemand;
  Double daReceivingMw;
  Double maxDaSendingMw;
  Integer roundId;

}
