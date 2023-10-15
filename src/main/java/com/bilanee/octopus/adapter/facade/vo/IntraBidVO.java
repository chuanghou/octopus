package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.Operation;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraBidVO {

    /**
     * 挂牌Id
     */
    Long bidId;

    /**
     * 初始挂牌电力
     */
    Double quantity;

    /**
     * 报单方向
     */
    Direction direction;


    /**
     * 剩余挂牌电力
     */
    Double transit;

    /**
     * 挂牌价格
     */
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
    List<IntraDealVO> intraDealVOs;


}
