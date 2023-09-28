package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "resource_transaction_status", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionDO {

  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Integer resourceId;
  Integer resourceType;
  Integer marketType;
  Double clearedMw;

}
