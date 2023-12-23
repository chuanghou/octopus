package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("units_assigned_to_trader_results")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratorResult {

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
     * 机组id
     **/
    Integer unitId;

    /**
     * 机组名称
     */
    @TableField(exist = false)
    String unitName;

    /**
     * 该轮比赛省间中长期净收入（元）
     **/
    Double interForwardRevenue;
    /**
     * 该轮比赛省内中长期净收入（元）
     **/
    Double intraForwardRevenue;
    /**
     * 该轮比赛省间现货净收入（元）
     **/
    Double interSpotRevenue;
    /**
     * 该轮比赛省间差价合约收入（元）
     **/
    Double interRevenue;
    /**
     * 该轮比赛日前全电量结算收入（元）
     **/
    Double daRevenue;
    /**
     * 该轮比赛实时偏差电量收入（元）
     **/
    Double rtDeviationRevenue;
    /**
     * 该轮比赛运行成本（元）
     **/
    Double outputCost;
    /**
     * 该轮比赛利润（元）
     **/
    Double profit;
    /**
     * 该轮比赛归一化利润（元）
     **/
    Double normalizedProfit;
    /**
     * 同类机组的平均中长期收入（元）
     **/
    Double averageForwardRevenueOfSameType;
    /**
     * 同类机组的平均日前全电量结算收入（元）
     **/
    Double averageDaRevenueOfSameType;
    /**
     * 同类机组的平均运行成本（元）
     **/
    Double averageOutputCostOfSameType;
    /**
     * 同类机组的平均利润（元）
     **/
    Double averageProfitOfSameType;
    /**
     * 机组所分摊的实时现货阻塞费用支出（元）
     **/
    @JsonProperty("rRtCongestionShare")
    Double rRtCongestionShare;
    /**
     * 机组运行补偿费用（元）
     **/
    @JsonProperty("rUnitCompensation")
    Double rUnitCompensation;
    /**
     * 返还给机组的省间双轨制不平衡资金收入（元）
     **/
    @JsonProperty("rInterImbalanceShare")
    Double rInterImbalanceShare;
    /**
     * 返还给机组的省内双轨制不平衡资金收入（元）
     **/
    @JsonProperty("rIntraImbalanceShare")
    Double rIntraImbalanceShare;

}
