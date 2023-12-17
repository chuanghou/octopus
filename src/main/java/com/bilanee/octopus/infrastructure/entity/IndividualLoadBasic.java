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
@TableName("individual_load_basic")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IndividualLoadBasic {

  @TableId(type = IdType.INPUT)
  Integer loadId;
  String loadName;
  Integer prov;
  Double maxP;
  Boolean isMarketLoad;
  Integer nodeId;
}
