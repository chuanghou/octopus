package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.DoubleSerialize;
import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.enums.InstantStatus;
import com.bilanee.octopus.basic.enums.Operation;
import com.bilanee.octopus.basic.enums.UnitType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitRollBidVO {

    /**
     * 单元id
     */
    Long unitId;


    /**
     * 容量
     */
    Double capacity;

    /**
     * 价格约束
     */
    GridLimit priceLimit;

    /**
     * 单元名
     */
    String unitName;

    /**
     * 单元类型
     */
    UnitType unitType;


    /**
     * 原始数据库里面的单元id
     */
    Integer sourceId;

    /**
     * 已持仓电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double position;

    /**
     * 在挂待成交电力
     */
    @JsonSerialize(using = DoubleSerialize.class)
    Double transit;

    /**
     * 持仓限制
     */
    List<BalanceVO> balanceVOs;

    /**
     * 报单及成交结果详情
     */
    RollBidVO rollBidVO;

    /**
     * 挂牌状态
     */
    InstantStatus instantStatus;

    /**
     * 操作
     */
    List<Operation> operations;

}
