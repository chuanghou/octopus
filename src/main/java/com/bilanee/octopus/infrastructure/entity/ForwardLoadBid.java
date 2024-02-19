package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
  @TableField("annual_bid_mw_1")
  Double annualBidMw1;
  @TableField("annual_bid_mw_2")
  Double annualBidMw2;
  @TableField("annual_bid_mw_3")
  Double annualBidMw3;
  @TableField("annual_bid_price_1")
  Double annualBidPrice1;
  @TableField("annual_bid_price_2")
  Double annualBidPrice2;
  @TableField("annual_bid_price_3")
  Double annualBidPrice3;
  @TableField("monthly_bid_mw_1")
  Double monthlyBidMw1;
  @TableField("monthly_bid_mw_2")
  Double monthlyBidMw2;
  @TableField("monthly_bid_mw_3")
  Double monthlyBidMw3;
  @TableField("monthly_bid_price_1")
  Double monthlyBidPrice1;
  @TableField("monthly_bid_price_2")
  Double monthlyBidPrice2;
  @TableField("monthly_bid_price_3")
  Double monthlyBidPrice3;
  Integer loadId;

}
