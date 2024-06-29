package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.Operation;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RollBidVO {

    /**
     * 挂牌Id
     */
    Long bidId;

    /**
     * 初始挂牌电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double quantity;

    /**
     * 报单方向
     */
    Direction direction;


    /**
     * 剩余挂牌电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double transit;

    /**
     * 已撤电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double cancelled;

    /**
     * 挂牌价格
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double price;

    /**
     * 挂牌时间
     */
    Long declareTimeStamp;

    /**
     * 撤单时间
     */
    Long cancelTimeStamp;

    /**
     * 挂牌状态
     */
    BidStatus bidStatus;

    /**
     * 操作
     */
    List<Operation> operations;

    /**
     * 成交详细
     */
    List<RollDealVO> rollDealVOs;


}
