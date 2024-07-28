package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "retail_plan_load_bid_and_result", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RetailMarketLoadBidDO {

    @TableId(type = IdType.AUTO)
    Integer id;
    Integer roundId;
    Integer loadId;
    Date dt;
    Integer retailPlanId;
    Boolean isSelectedRetailPlan;
}