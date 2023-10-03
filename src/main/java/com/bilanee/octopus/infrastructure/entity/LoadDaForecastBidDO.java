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
@TableName(value = "intraprovincial_spot_load_bid",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoadDaForecastBidDO {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Integer bidId;
  Double bidMw;
  Double bidPrice;
  Integer loadId;

}
