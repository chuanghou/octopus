package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("market_setting")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MarketSettingDO {

    @TableId(type = IdType.INPUT)
    Integer marketSettingId;
//    String objective_type;
//    String is_network_loss;
//    String is_inplant_use;
//    String prd_num;
//    String is_startup_cost;
//    String is_ramp_constraint;
//    String is_max_on_off_times_constraint;
//    String is_min_on_off_duration_constraint;
//    String is_branch_constraint;
//    String is_section_constraint;
//    String is_sys_res_constraint;
//    String is_unitgroup_MWh_constraint;
//    String is_unitgroup_MW_constraint;
//    String is_unitgroup_res_constraint;
//    String is_entering_review_stage;
//    String is_conducting_answering_module;
    Double offerPriceCap;
    Double offerPriceFloor;
    Double bidPriceCap;
    Double bidPriceFloor;
//    String balance_constraint_penalty_factor;
//    String branch_constraint_penalty_factor;
//    String section_constraint_penalty_factor;
    Double loadAnnualMaxForecastErr;
    Double loadMonthlyMaxForecastErr;
    Double loadDaMaxForecastErr;
    Double renewableAnnualMaxForecastErr;
    Double renewableMonthlyMaxForecastErr;
    Double renewableDaMaxForecastErr;
//    String forward_num_offer_segs;
//    String forward_num_bid_segs;
//    String spot_num_offer_segs;
    Integer spotNumBidSegs;
//    String thermal_forecast_confidence;
//    String load_forecast_confidence;
//    String renewable_annual_forecast_confidence;
//    String renewable_monthly_forecast_confidence;
//    String renewable_da_forecast_confidence;
//    String max_startup_curve_prds;
//    String max_shutdown_curve_prds;
//    String trader_num;
//    String robot_num;
    Integer roundId;
//    String round_num;
//    String market_type;
    Integer intraprovincialAnnualBidDuration;
    Integer intraprovincialMonthlyBidDuration;
    Integer intraprovincialSpotBidDuration;
    Integer interprovincialAnnualBidDuration;
    Integer interprovincialMonthlyBidDuration;
    Integer interprovincialSpotBidDuration;
//    String dt;
//    String max_load_coe_send;
//    String min_load_coe_send;
//    String max_load_coe_receive;
//    String min_load_coe_receive;
//    String max_wind_coe_send;
//    String min_wind_coe_send;
//    String max_wind_coe_receive;
//    String min_wind_coe_receive;
    Double transmissionAndDistributionTariff;
    Double regulatedUserTariff;
    Double regulatedProducerPrice;
    Double regulatedInterprovTransmissionPrice;
    @Getter(AccessLevel.NONE)
    Integer interprov_trading_mode;
    @Getter(AccessLevel.NONE)
    Integer interprov_clearing_mode;
//    String is_setting_default_offer_for_traders;
//    String paper_id;
    Integer intraprovincialAnnualResultDuration;
    Integer intraprovincialMonthlyResultDuration;
    Integer intraprovincialSpotResultDuration;
    Integer interprovincialAnnualResultDuration;
    Integer interprovincialMonthlyResultDuration;
    Integer interprovincialSpotResultDuration;
    Integer settleResultDuration;

    static private Map<Integer, String> map0 = Collect.asMap(
            1, "政府定价定量",
            2, "政府定量不定价",
            3, "政府干预量不定价"
    );

    public String getInterprov_trading_mode() {

        return map0.get(interprov_trading_mode);
    }


    static private Map<Integer, String> map1 = Collect.asMap(
            1, "边际统一出清",
            2, "按匹配对分别出清"
    );
    public String getInterprov_clearing_mode() {
        return map1.get(interprov_clearing_mode);
    }

}
