package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.*;
import com.stellariver.milky.domain.support.base.BaseDataObject;
import com.stellariver.milky.infrastructure.base.database.AbstractMpDO;
import com.stellariver.milky.infrastructure.base.database.ListJsonHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_bid_do",autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidDO extends AbstractMpDO  {

    @TableId(type = IdType.INPUT)
    Long bidId;
    Long compId;
    Integer roundId;
    Long unitId;
    Province province;
    TimeFrame timeFrame;
    TradeStage tradeStage;
    Direction direction;
    Double quantity;
    Double price;

    @TableField(typeHandler = DealHandlers.class)
    List<Deal> deals;
    Long declareTimeStamp;
    Long cancelledTimeStamp;
    BidStatus bidStatus;

    static public class DealHandlers extends ListJsonHandler<Deal> {};


}
