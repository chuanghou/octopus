package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.enums.Choice;
import com.bilanee.octopus.infrastructure.handlers.ListChoiceHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "retail_market_load_bid_and_result", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RetailMarketLoadBidDO {

    @TableId(type = IdType.AUTO)
    Integer id;
    Integer roundId;
    Integer loadId;
    Date dt;
    Integer retailPackageId;
    Boolean isSelectedRetailPackage;

}