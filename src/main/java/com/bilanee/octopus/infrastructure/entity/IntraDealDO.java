package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("intraprovincial_forward_bilateral_transaction_result")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraDealDO {

  Long id;
  Integer roundId;
  Integer buyerId;
  Integer buyerType;
  Integer sellerId;
  Integer sellerType;
  String dt;
  Integer pfvPrd;
  Integer marketType;
  Date transTime;
  Double clearedMw;
  Double clearedPrice;

}
