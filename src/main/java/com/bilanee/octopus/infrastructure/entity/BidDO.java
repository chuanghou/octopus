package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.enums.*;
import com.stellariver.milky.infrastructure.base.database.ListJsonHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_bid_do",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidDO {

    @TableId(type = IdType.AUTO)
    Long bidId;
    String userId;
    Long compId;
    Integer roundId;
    Long unitId;
    Province province;
    TimeFrame timeFrame;
    Integer instant;
    TradeStage tradeStage;
    Direction direction;
    Double quantity;
    Double price;

    @TableField(typeHandler = DealHandlers.class)
    List<Deal> deals;
    Long declareTimeStamp;
    Long cancelledTimeStamp;
    BidStatus bidStatus;
    Double closeBalance;

    static public class DealHandlers extends ListJsonHandler<Deal> {};


}
