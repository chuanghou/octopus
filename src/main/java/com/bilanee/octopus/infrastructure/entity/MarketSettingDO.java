package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "market_setting",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MarketSettingDO {

  @TableId(type = IdType.INPUT)
  Integer marketSettingId;
  Integer objectiveType;
  Integer prdNum;
  Boolean isEnteringReviewStage;
  Boolean isOpeningTradingPlatform;
  Boolean isConductingQAndAModule;
  Double offerPriceCap;
  Double offerPriceFloor;
  Double bidPriceCap;
  Double bidPriceFloor;
  Double clearedPriceCap;
  Integer balanceConstraintPenaltyFactor;
  Integer branchConstraintPenaltyFactor;
  Integer sectionConstraintPenaltyFactor;

  @TableField(typeHandler = JacksonTypeHandler.class)
  ForecastErr loadMaxForecastErr;

  @TableField(typeHandler = JacksonTypeHandler.class)
  ForecastErr renewableMaxForecastErr;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr multiYearLoadForecastDeviation;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr annualLoadForecastDeviation;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr monthlyLoadForecastDeviation;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr daLoadForecastDeviation;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr multiYearRenewableForecastDeviation;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr annualRenewableForecastDeviation;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr monthlyRenewableForecastDeviation;

  @TableField(typeHandler = JacksonTypeHandler.class)
  RealErr daRenewableForecastDeviation;


  Integer forwardNumOfferSegs;
  Integer forwardNumBidSegs;
  Integer spotNumOfferSegs;
  Integer spotNumBidSegs;
  Integer maxStartupCurvePrds;
  Integer maxShutdownCurvePrds;
  Integer traderNum;
  Integer robotNum;
  Integer roundId;
  Integer roundNum;
  Integer intraprovincialAnnualBidDuration;
  Integer intraprovincialMonthlyBidDuration;
  Integer intraprovincialSpotBidDuration;
  Integer interprovincialAnnualBidDuration;
  Integer interprovincialMonthlyBidDuration;
  Integer interprovincialSpotBidDuration;
  Integer interprovincialAnnualResultDuration;
  Integer intraprovincialAnnualResultDuration;
  Integer interprovincialMonthlyResultDuration;
  Integer intraprovincialMonthlyResultDuration;
  Integer interprovincialSpotResultDuration;
  Integer intraprovincialSpotResultDuration;
  Integer intraprovincialSpotRollingBidDuration;
  Integer intraprovincialSpotRollingResultDuration;
  Integer settleResultDuration;
  Integer reviewDuration;
  Integer quizCompeteDuration;
  Integer quizResultDuration;
  String dt;
  String caseSetting;
  String transmissionAndDistributionTariff;
  Double regulatedUserTariff;
  Double regulatedProducerPrice;
  Double regulatedInterprovTransmissionPrice;
  Double capacityPrice;
  String interprovTradingMode;
  Integer interprovClearingMode;
  Integer traderOfferMode;
  Integer robotOfferMode;
  Integer paperId;
  Double maxForwardUnitPositionInterest;
  Double maxForwardLoadPositionInterest;
  String multiYearCoalPrice;
  String annualCoalPrice;
  String monthlyCoalPrice;
  String daCoalPrice;
  Double retailPriceForecastMultiple;
  Integer assetAllocationMode;
  String assetAllocationModeStr;

  Boolean isOpeningIntraprovSpotQuickOffer;
  Boolean isOpeningThermalStartOffer;
  Boolean isOpeningThermalMinoutputOffer;

  /**
   * 使能单点登录限制
   */
  Boolean singleLoginLimit;

  /**
   * 发电侧中长期持仓考核要求（%）
   */
  Double minForwardUnitPosition;
  /**
   * 用户侧中长期持仓考核要求（%）
   */
  Double minForwardLoadPosition;
  /**
   * 各设备省间可交易额度相对于按容量均分的倍数
   */
  Double maxForwardClearedMwMultiple;

  /**
   * 省内多年报价持续时长（min）
   */
  Integer intraprovincialMultiYearBidDuration;

  /**
   * 省内多年结果查询时长（min）
   */
  Integer intraprovincialMultiYearResultDuration;

  /**   * 新能源专场交易电网申报需求占新能源预测上网电量百分比“:”为分隔符，存储顺序：
   * 送电省风、送电省光、
   * 受电省风、受电省光
   */
  String renewableSpecialTransactionDemandPercentage;

  /**
   * 新能源专场交易电网申报需求占新能源预测上网电量百分比“:”为分隔符，存储顺序：
   * 送电省风、送电省光、
   * 受电省风、受电省光
   */
  String renewableSpecialTransactionDemand;


  /**
   * 总用电量中可变更零售套餐的
   * 电量占比
   */
  Double mwhPercentageForRetailPlan;


  /**
   * 用户侧零售套餐说明
   */
  String retailPlanDescription;


  /**
   * 风力新能源价格上限
   */
  Double windSpecificPriceCap;


  /**
   * 光伏新能源价格上限
   */
  Double solarSpecificPriceCap;


}
