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
@TableName("intraprovincial_multi_year_pool_transaction_result")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MultiYearPoolTransactionDO {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  String dt;
  Double clearedMwh;
  Double clearedPrice;
  Integer unitId;

}
