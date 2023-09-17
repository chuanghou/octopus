package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.adapter.facade.vo.BalanceVO;
import com.bilanee.octopus.adapter.facade.vo.IntraBidVO;
import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.enums.UnitType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitIntraBidVO {

    /**
     * 单元id
     */
    Long unitId;

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
    Double position;

    /**
     * 在挂待成交电力
     */
    Double transit;

    /**
     * 持仓限制
     */
    List<BalanceVO> balanceVOs;

    /**
     * 报单及成交结果详情
     */
    List<IntraBidVO> intraBidVOs;

}
