package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
  Boolean isNetworkLoss;
  Boolean isInplantUse;
  Integer prdNum;
  Boolean isStartupCost;
  Boolean isRampConstraint;
  Boolean isMaxOnOffTimesConstraint;
  Boolean isMinOnOffDurationConstraint;
  Boolean isBranchConstraint;
  Boolean isSectionConstraint;
  Boolean isSysResConstraint;
  @TableField("is_unitgroup_MWh_constraint")
  Boolean isUnitgroupMWhConstraint;
  @TableField("is_unitgroup_MW_constraint")
  Boolean isUnitgroupMwConstraint;
  Boolean isUnitgroupResConstraint;
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
  Double loadAnnualMaxForecastErr;
  Double loadMonthlyMaxForecastErr;
  Double loadDaMaxForecastErr;
  Double renewableAnnualMaxForecastErr;
  Double renewableMonthlyMaxForecastErr;
  Double renewableDaMaxForecastErr;
  Integer forwardNumOfferSegs;
  Integer forwardNumBidSegs;
  Integer spotNumOfferSegs;
  Integer spotNumBidSegs;
  Double thermalForecastConfidence;
  Double loadForecastConfidence;
  Double renewableAnnualForecastConfidence;
  Double renewableMonthlyForecastConfidence;
  Double renewableDaForecastConfidence;
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
  String annualCoalPrice;
  String monthlyCoalPrice;
  String daCoalPrice;
  String forecastDeviation;
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


}
