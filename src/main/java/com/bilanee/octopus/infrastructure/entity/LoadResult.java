package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("loads_assigned_to_trader_results")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoadResult {

    @TableId(type = IdType.INPUT)
    Long id;
    /**
     * 当前比赛轮次
     **/
    Integer roundId;
    /**
     * 日期
     **/
    String dt;
    /**
     * 交易员id
     **/
    String traderId;
    /**
     * 负荷id
     **/
    Integer loadId;
    /**
     * 该轮比赛省间中长期净支出（元）
     **/
    Double interForwardExpenditure;
    /**
     * 该轮比赛省内中长期净支出（元）
     **/
    Double intraForwardExpenditure;
    /**
     * 该轮比赛日前全电量结算支出（元）
     **/
    Double daExpenditure;
    /**
     * 该轮比赛实时偏差电量支出（元）
     **/
    Double rtDeviationExpenditure;
    /**
     * 该轮比赛支出（元）
     **/
    Double expenditure;
    /**
     * 该轮比赛归一化支出（元）
     **/
    Double normalizedExpenditure;
    /**
     * 同类负荷的平均支出（元）
     **/
    Double averageExpenditureOfSameType;
    /**
     * 用户分摊的省内双轨制不平衡资金支出（元）
     **/
    Double rIntraImbalanceShare;
    /**
     * 用户分摊的省间双轨制不平衡资金支出（元）
     **/
    Double rInterImbalanceShare;
    /**
     * 用户分摊的机组运行补偿费用支出（元）
     **/
    Double rUnitCompensationShare;
    /**
     * 用户侧偏差收益回收资金（元）
     **/
    Double rDeviationOutgoing;
    /**
     * 用户侧偏差收益返还资金（元）
     **/
    Double rDeviationIncoming;


}
