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
@TableName(value = "interprovincial_forward_pool_transaction_result",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterDealDO {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  Integer resourceId;
  Integer resourceType;
  String dt;
  Integer pfvPrd;
  Integer marketType;
  Double clearedMw;
  Double clearedPrice;

}
