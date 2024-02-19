package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "interprovincial_forward_unit_offer",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForwardUnitOffer {

  Long id;
  Integer roundId;
  String dt;
  Integer pfvPrd;
  @TableField("annual_offer_mw_1")
  Double annualOfferMw1;
  @TableField("annual_offer_mw_2")
  Double annualOfferMw2;
  @TableField("annual_offer_mw_3")
  Double annualOfferMw3;
  @TableField("annual_offer_price_1")
  Double annualOfferPrice1;
  @TableField("annual_offer_price_2")
  Double annualOfferPrice2;
  @TableField("annual_offer_price_3")
  Double annualOfferPrice3;
  @TableField("monthly_offer_mw_1")
  Double monthlyOfferMw1;
  @TableField("monthly_offer_mw_2")
  Double monthlyOfferMw2;
  @TableField("monthly_offer_mw_3")
  Double monthlyOfferMw3;
  @TableField("monthly_offer_price_1")
  Double monthlyOfferPrice1;
  @TableField("monthly_offer_price_2")
  Double monthlyOfferPrice2;
  @TableField("monthly_offer_price_3")
  Double monthlyOfferPrice3;
  Integer unitId;

}
