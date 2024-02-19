package com.bilanee.octopus.infrastructure.entity;

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
  Double annualOfferMw1;
  Double annualOfferMw2;
  Double annualOfferMw3;
  Double annualOfferPrice1;
  Double annualOfferPrice2;
  Double annualOfferPrice3;
  Double monthlyOfferMw1;
  Double monthlyOfferMw2;
  Double monthlyOfferMw3;
  Double monthlyOfferPrice1;
  Double monthlyOfferPrice2;
  Double monthlyOfferPrice3;
  Integer unitId;

}
