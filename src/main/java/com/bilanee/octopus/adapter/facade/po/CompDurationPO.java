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
public class CompDurationPO {

    @NotNull @Positive
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



}
