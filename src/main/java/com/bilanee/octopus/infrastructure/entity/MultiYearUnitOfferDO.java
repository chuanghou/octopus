package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("intraprovincial_multi_year_unit_offer")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MultiYearUnitOfferDO {


    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 当前比赛轮次
     */
    Integer roundId;

    /**
     * 机组id
     */
    Integer unitId;
    /**
     * 日期
     */
    Date dt;

    /**
     * 省内多年成交上限（MWh）
     */
    Double maxMultiYearClearedMwh;

    /**
     * 多年交易报价段1申报数量（MWh）
     */
    @TableField("offer_mwh_1")
    Double offerMwh1;

    /**
     * 多年交易报价段2申报数量（MWh）
     */
    @TableField("offer_mwh_2")
    Double offerMwh2;

    /**
     * 多年交易报价段3申报数量（MWh）
     */
    @TableField("offer_mwh_3")
    Double offerMwh3;

    /**
     * 多年交易报价段1申报价格（元/MWh）
     */
    @TableField("offer_price_1")
    Double offerPrice1;

    /**
     * 多年交易报价段2申报价格（元/MWh）
     */
    @TableField("offer_price_2")
    Double offerPrice2;

    /**
     * 多年交易报价段3申报价格（元/MWh）
     */
    @TableField("offer_price_3")
    Double offerPrice3;


}
