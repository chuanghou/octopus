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
  Boolean isConductingQAndAModule;
  Double offerPriceCap;
  Double offerPriceFloor;
  Double bidPriceCap;
  Double bidPriceFloor;
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
  Integer settleResultDuration;
  Integer reviewDuration;
  String dt;
  Double transmissionAndDistributionTariff;
  Double regulatedUserTariff;
  Double regulatedProducerPrice;
  Double regulatedInterprovTransmissionPrice;
  Integer interprovTradingMode;
  Integer interprovClearingMode;
  Boolean isSettingDefaultOfferForTraders;
  Boolean isSettingDefaultOfferForRobots;
  Integer paperId;
  Double maxForwardUnitPositionInterest;
  Double maxForwardLoadPositionInterest;
  String caseSetting;
  Double coalPriceMultiple;
  Integer quizCompeteDuration;
  Integer quizResultDuration;
  String forecastDeviation;
}
