package com.bilanee.octopus.adapter.facade.po;

import com.stellariver.milky.common.base.Valids;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.*;
import java.util.List;

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
    @NotNull @PositiveOrZero
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

    @NotNull
    Integer roundNum;


    /**
     * 设备分配方式
     */
    @NotNull @Min(value = 1) @Max(value = 6)
    Integer assetAllocationMode;

    List<String> assetAllocationModes;

    @NotNull(message = "轮次相关参数不能为空") @Valids
    List<RoundSetting> roundSettings;

    /**
     * 是否开放省内现货快捷报价
     */
    Boolean isOpeningIntraprovSpotQuickOffer;

    /**
     * 是否开放火电机组开机和空载费用报价
     */
    Boolean isOpeningThermalStartOffer;

    /**
     * 是否开放火电机组最小技术出力报价
     */
    Boolean isOpeningThermalMinoutputOffer;

    /**
     * 省内日滚动报价持续时长（min）
     */
    Integer intraprovincialSpotRollingBidDuration;

    /**
     * 省内日滚动结果查询时长（min）
     */
    Integer intraprovincialSpotRollingResultDuration;

    /**
     * 省内多年报价持续时长（min）
     */
    Integer intraprovincialMultiYearBidDuration;

    /**
     * 省内多年结果查询时长（min）
     */
    Integer intraprovincialMultiYearResultDuration;

    /**
     * 新能源专场交易电网申报需求占新能源预测上网电量百分比
     */
    RenewableSpecialTransactionDemandPercentage renewableSpecialTransactionDemandPercentage;

    /**
     * 总用电量中可变更零售套餐的
     * 电量占比
     */
    Double mwhPercentageThatCanBeChangedForRetailPackage;

    /**
     * 用户侧零售套餐说明
     */
    String retailPlanDescription;


    /**
     * 风电新能源价格上限
     */
    Double windSpecificPriceCap;


    /**
     * 光伏新能源价格上限
     */
    Double solarSpecificPriceCap;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class RenewableSpecialTransactionDemandPercentage {

        /**
         * 送电省风
         */
        Double transferWind;

        /**
         * 送电省光
         */
        Double transferSolar;

        /**
         * 受电省风
         */
        Double receiverWind;

        /**
         * 受电省光
         */
        Double receiverSolar;

        public String storeValue() {
            return String.format("%s:%s:%s:%s", transferWind, transferSolar, receiverWind, receiverSolar);
        }

        static public RenewableSpecialTransactionDemandPercentage resolve(String value) {
            String[] split = StringUtils.split(value, ":");
            RenewableSpecialTransactionDemandPercentage resolved = new RenewableSpecialTransactionDemandPercentage();
            resolved.setTransferWind(Double.parseDouble(split[0]));
            resolved.setTransferSolar(Double.parseDouble(split[1]));
            resolved.setReceiverWind(Double.parseDouble(split[2]));
            resolved.setReceiverSolar(Double.parseDouble(split[3]));
            return resolved;
        }

    }

}

