package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "tie_line_power", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TieLinePowerDO {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Double annualTielinePower;
  Double monthlyTielinePower;
  Double daTielinePower;
  Double annualMarketTielinePower;
  Double monthlyMarketTielinePower;
  Double daMarketTielinePower;
  Double annualNonmarketTielinePower;
  Double monthlyNonmarketTielinePower;
  Double daNonmarketTielinePower;
  Boolean isInterprovincialSpotTransaction;
  Integer tielineId;

}
