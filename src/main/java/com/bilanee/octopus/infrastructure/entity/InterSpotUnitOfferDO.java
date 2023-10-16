package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("interprovincial_spot_unit_offer")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterSpotUnitOfferDO {

  @TableId(type = IdType.INPUT)
  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  @TableField("spot_offer_mw_1")
  Double spotOfferMw1;
  @TableField("spot_offer_mw_2")
  Double spotOfferMw2;
  @TableField("spot_offer_mw_3")
  Double spotOfferMw3;
  @TableField("spot_offer_price_1")
  Double spotOfferPrice1;
  @TableField("spot_offer_price_2")
  Double spotOfferPrice2;
  @TableField("spot_offer_price_3")
  Double spotOfferPrice3;
  Integer unitId;

}
