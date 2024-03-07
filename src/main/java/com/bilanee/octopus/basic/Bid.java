package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.infrastructure.entity.MarketSettingDO;
import com.bilanee.octopus.infrastructure.mapper.MarketSettingMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.tool.Doubles;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bid {

    Long bidId;
    String userId;
    Long compId;
    Long unitId;
    Province province;
    TimeFrame timeFrame;
    Integer roundId;
    TradeStage tradeStage;
    Direction direction;
    Double quantity;
    Double price;
    Long declareTimeStamp;
    @Builder.Default
    List<Deal> deals = new ArrayList<>();
    Long cancelledTimeStamp;
    BidStatus bidStatus;
    Double closeBalance;

    public Double getTransit() {
        if ((bidStatus != BidStatus.MANUAL_CANCELLED) && (bidStatus != BidStatus.SYSTEM_CANCELLED)) {
            return quantity - deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
        } else {
            return 0D;
        }
    }

    static Cache<String, Double> cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

    @SneakyThrows
    public Double getPriceAfterTariff() {
        return Doubles.subtract(price, getTariff());
    }

    @SneakyThrows
    public Double getTariff() {
        if (direction == Direction.SELL) {
            return 0D;
        } else {
            return cache.get("TransmissionAndDistributionTariff", () -> {
                MarketSettingMapper marketSettingMapper = BeanUtil.getBean(MarketSettingMapper.class);
                MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
                return marketSettingDO.getTransmissionAndDistributionTariff();
            });
        }

    }


    public Double getCancelled() {
        if ((bidStatus != BidStatus.MANUAL_CANCELLED) && (bidStatus != BidStatus.SYSTEM_CANCELLED)) {
            return 0D;
        } else {
            return quantity - deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
        }
    }


}
