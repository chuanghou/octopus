package com.bilanee.octopus.adapter.facade.po;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.infrastructure.entity.MultiYearUnitOfferDO;
import com.bilanee.octopus.infrastructure.mapper.MarketSettingMapper;
import com.bilanee.octopus.infrastructure.mapper.MultiYearUnitOfferDOMapper;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.ErrorEnum;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MultiYearBid {

    @NotNull(message = "机组id不能为空")
    Long unitId;

    String unitName;

    /**
     * 省内多年成交上限（MWh）， offerMwh1 + offerMwh2 + offerMwh3 不能超过这个值
     */
    Double maxMultiYearClearedMwh;

    /**
     * 多年交易报价段1申报数量（MWh）
     */
    @NotNull(message = "申报数量不能为空") @PositiveOrZero(message = "申报数量大于等于0")
    Double offerMwh1;

    /**
     * 多年交易报价段2申报数量（MWh）
     */
    @NotNull(message = "申报数量不能为空") @PositiveOrZero(message = "申报数量大于等于0")
    Double offerMwh2;

    /**
     * 多年交易报价段3申报数量（MWh）
     */
    @NotNull(message = "申报数量不能为空") @PositiveOrZero(message = "申报数量大于等于0")
    Double offerMwh3;

    /**
     * 多年交易报价段1申报价格（元/MWh）
     */
    @NotNull(message = "价格不能为空") @PositiveOrZero(message = "价格必须大于等于0")
    Double offerPrice1;

    /**
     * 多年交易报价段2申报价格（元/MWh）
     */
    @NotNull(message = "价格不能为空") @PositiveOrZero(message = "价格必须大于等于0")
    Double offerPrice2;

    /**
     * 多年交易报价段3申报价格（元/MWh）
     */
    @NotNull(message = "价格不能为空") @PositiveOrZero(message = "价格必须大于等于0")
    Double offerPrice3;


    @AfterValidation
    public void afterValidation() {
        double v = offerMwh1 + offerMwh2 + offerMwh3;
        LambdaQueryWrapper<MultiYearUnitOfferDO> eq = new LambdaQueryWrapper<MultiYearUnitOfferDO>()
                .eq(MultiYearUnitOfferDO::getRoundId, roundId)
                .eq(MultiYearUnitOfferDO::getUnitId, unitId);
        MultiYearUnitOfferDO multiYearUnitOfferDO = BeanUtil.getBean(MultiYearUnitOfferDOMapper.class).selectOne(eq);
        BizEx.trueThrow(v > multiYearUnitOfferDO.getMaxMultiYearClearedMwh(), ErrorEnums.PARAM_FORMAT_WRONG.message("报单量超过限制"));

        Double newEnergyUpLimit = BeanUtil.getBean(MarketSettingMapper.class).selectById(1).getNewEnergyUpLimit();
        BizEx.trueThrow(offerPrice1 > newEnergyUpLimit, ErrorEnums.PARAM_FORMAT_WRONG.message("价格超过最大限制" + newEnergyUpLimit));
        BizEx.trueThrow(offerPrice2 > newEnergyUpLimit, ErrorEnums.PARAM_FORMAT_WRONG.message("价格超过最大限制" + newEnergyUpLimit));
        BizEx.trueThrow(offerPrice3 > newEnergyUpLimit, ErrorEnums.PARAM_FORMAT_WRONG.message("价格超过最大限制" + newEnergyUpLimit));

    }

}
