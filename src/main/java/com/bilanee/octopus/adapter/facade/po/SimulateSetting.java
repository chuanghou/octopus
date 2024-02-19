package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimulateSetting {

    /**
     * 省内年度报价持续时长（min）
     **/
    @NotNull @Positive
    Integer intraprovincialAnnualBidDuration;
    /**
     * 省内月度报价持续时长（min）
     **/
    @NotNull @Positive
    Integer intraprovincialMonthlyBidDuration;
    /**
     * 省内现货报价持续时长（min）
     **/
    @NotNull @Positive
    Integer intraprovincialSpotBidDuration;
    /**
     * 省间年度报价持续时长（min）
     **/
    @NotNull @Positive
    Integer interprovincialAnnualBidDuration;
    /**
     * 省间月度报价持续时长（min）
     **/
    @NotNull @Positive
    Integer interprovincialMonthlyBidDuration;
    /**
     * 省间现货报价持续时长（min）
     **/
    @NotNull @Positive
    Integer interprovincialSpotBidDuration;
    /**
     * 省间年度结果查询时长（min）
     **/
    @NotNull @Positive
    Integer interprovincialAnnualResultDuration;
    /**
     * 省内年度结果查询时长（min）
     **/
    @NotNull @Positive
    Integer intraprovincialAnnualResultDuration;
    /**
     * 省间月度结果查询时长（min）
     **/
    @NotNull @Positive
    Integer interprovincialMonthlyResultDuration;
    /**
     * 省内月度结果查询时长（min）
     **/
    @NotNull @Positive
    Integer intraprovincialMonthlyResultDuration;
    /**
     * 省间现货结果查询时长（min）
     **/
    @NotNull @Positive
    Integer interprovincialSpotResultDuration;
    /**
     * 省内现货结果查询时长（min）
     **/
    @NotNull @Positive
    Integer intraprovincialSpotResultDuration;
    /**
     * 结算结果查询时长（min）
     **/
    @NotNull @Positive
    Integer settleResultDuration;
    /**
     * 复盘查询时长（min）
     **/
    @NotNull @Positive
    Integer reviewDuration;

    /**
     * 交易员数量
     */
    @NotNull @Positive
    Integer traderNum;

    /**
     * 机器人数量
     */
    @NotNull @Positive
    Integer robotNum;

    /**
     * 交易员报价模式 1零量零价型，2预测电价型
     */
    @NotNull
    Integer traderOfferMode;

    /**
     * 机器人报价模式 1零量零价型，2预测电价型
     */
    @NotNull
    Integer robotOfferMode;

    /**
     * 是否进入复盘阶段
     */
    @NotNull
    Boolean isEnteringReviewStage;
}
