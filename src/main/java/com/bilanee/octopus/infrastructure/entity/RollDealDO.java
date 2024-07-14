package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("intraprovincial_spot_rolling_transaction_result")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RollDealDO {

  Integer roundId;
  Integer buyerId;
  Integer buyerType;
  Integer sellerId;
  Integer sellerType;
  String dt;
  Integer prd;
  Date transTime;
  Double clearedMw;
  Double clearedPrice;

}
