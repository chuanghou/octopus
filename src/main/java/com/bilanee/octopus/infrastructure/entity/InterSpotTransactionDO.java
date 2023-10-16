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
@TableName("interprovincial_spot_pool_transaction_result")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterSpotTransactionDO {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  Integer sellerId;
  String dt;
  Integer prd;
  Double clearedMw;
  Double clearedPrice;

}
