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
     * 负荷名称
     */
    @TableField(exist = false)
    String unitName;

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
     * 该轮比赛化支出（元）
     **/
    Double expenditure;
    /**
     * 同类负荷的平均支出（元）
     **/
    Double averageExpenditureOfSameType;
    /**
     * 用户分摊的省内双轨制不平衡资金支出（元）
     **/
    @JsonProperty("rIntraImbalanceShare")
    Double rIntraImbalanceShare;
    /**
     * 用户分摊的省间双轨制不平衡资金支出（元）
     **/
    @JsonProperty("rInterImbalanceShare")
    Double rInterImbalanceShare;
    /**
     * 用户分摊的机组运行补偿费用支出（元）
     **/
    @JsonProperty("rUnitCompensationShare")
    Double rUnitCompensationShare;
    /**
     * 用户侧偏差收益回收资金（元）
     **/
    @JsonProperty("rDeviationOutgoing")
    Double rDeviationOutgoing;
    /**
     * 用户侧偏差收益返还资金（元）
     **/
    @JsonProperty("rDeviationIncoming")
    Double rDeviationIncoming;


    /**
     * 该轮比赛收入（元）
     */
    Double revenue;

    /**
     * 该轮比赛利润（元）
     */
    Double profit;

    /**
     * 该轮比赛归一化利润（元）
     */
    Double normalizedProfit;


    /**
     * 该轮比赛所有差价合约净支出(元)
     */
    Double forwardContractExpenditure;


    /**
     * 同类负荷的平均利润
     */
    Double averageProfitOfSameType;


    /**
     * 该轮比赛所有差价合约净支出(元)
     */
    Double averageForwardContractExpenditureOfSameType;

}
