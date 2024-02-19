package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "interprovincial_forward_load_bid",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForwardLoadBid {

  Long id;
  Integer roundId;
  String dt;
  Integer pfvPrd;
  Double annualBidMw1;
  Double annualBidMw2;
  Double annualBidMw3;
  Double annualBidPrice1;
  Double annualBidPrice2;
  Double annualBidPrice3;
  Double monthlyBidMw1;
  Double monthlyBidMw2;
  Double monthlyBidMw3;
  Double monthlyBidPrice1;
  Double monthlyBidPrice2;
  Double monthlyBidPrice3;
  Integer loadId;

}
