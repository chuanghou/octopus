package com.bilanee.octopus.adapter.tunnel;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.common.enums.BidStatus;
import com.bilanee.octopus.common.enums.Province;
import com.bilanee.octopus.common.enums.TradeStage;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.infrastructure.entity.BidDO;
import com.bilanee.octopus.infrastructure.entity.CompDO;
import com.bilanee.octopus.infrastructure.entity.MarketSettingDO;
import com.bilanee.octopus.infrastructure.entity.MetaUnitDO;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.bilanee.octopus.infrastructure.mapper.MarketSettingMapper;
import com.bilanee.octopus.infrastructure.mapper.MetaUnitDOMapper;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import lombok.RequiredArgsConstructor;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class Tunnel {

    final MetaUnitDOMapper metaUnitDOMapper;
    final BidDOMapper bidDOMapper;
    final DomainTunnel domainTunnel;
    final CompDOMapper compDOMapper;
    final MarketSettingMapper marketSettingMapper;

    public Map<String, List<MetaUnit>> assignMetaUnits(Integer roundId, List<String> userIds) {
        Map<String, List<MetaUnit>> metaUnitMap = new HashMap<>();

        for (int i = 0; i < userIds.size(); i++) {
            List<Integer> sourceIds = AssignUtils.assignSourceId(roundId, userIds.size(), 30, i);
            LambdaQueryWrapper<MetaUnitDO> in = new LambdaQueryWrapper<MetaUnitDO>().in(MetaUnitDO::getSourceId, sourceIds);
            List<MetaUnitDO> metaUnitDOs = metaUnitDOMapper.selectList(in);
            metaUnitMap.put(userIds.get(i), Collect.transfer(metaUnitDOs, Convertor.INST::to));
        }

        return metaUnitMap;
    }

    public MetaUnit getMetaUnitById(Long id) {
        MetaUnitDO metaUnitDO = metaUnitDOMapper.selectById(id);
        return Convertor.INST.to(metaUnitDO);
    }


    public List<Bid> listBids(BidQuery q) {
        LambdaQueryWrapper<BidDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(q.getCompId() != null, BidDO::getCompId, q.getCompId());
        queryWrapper.eq(q.getUnitId() != null, BidDO::getUnitId, q.getUnitId());
        queryWrapper.eq(q.getRoundId() != null, BidDO::getRoundId, q.getRoundId());
        queryWrapper.eq(q.getProvince() != null, BidDO::getProvince, Kit.op(q.getProvince()).map(Province::name).orElse(null));
        queryWrapper.eq(q.getDirection() != null, BidDO::getDirection, Kit.op(q.getDirection()).map(Direction::name).orElse(null));
        queryWrapper.eq(q.getTradeStage() != null, BidDO::getTradeStage, Kit.op(q.getTradeStage()).map(TradeStage::name).orElse(null));
        queryWrapper.eq(q.getBidStatus() != null, BidDO::getBidStatus, Kit.op(q.getBidStatus()).map(BidStatus::name).orElse(null));
        List<BidDO> bidDOs = bidDOMapper.selectList(queryWrapper);
        return Collect.transfer(bidDOs, Convertor.INST::to);
    }

    public void coverBids(List<Bid> bids) {
        List<BidDO> bidDOs = Collect.transfer(bids, Convertor.INST::to);
        BidDO bidDO = bidDOs.get(0);
        LambdaQueryWrapper<BidDO> queryWrapper = new LambdaQueryWrapper<BidDO>().eq(BidDO::getCompId, bidDO.getCompId())
                .eq(BidDO::getRoundId, bidDO.getRoundId())
                .eq(BidDO::getTradeStage, bidDO.getTradeStage())
                .eq(BidDO::getUnitId, bidDO.getUnitId());
        bidDOMapper.selectList(queryWrapper).forEach(bidDOMapper::deleteById);
        bidDOs.forEach(bidDOMapper::insert);
    }

    public Comp runningComp() {
        LambdaQueryWrapper<CompDO> queryWrapper = new LambdaQueryWrapper<CompDO>().orderByDesc(CompDO::getCompId).last("LIMIT 1");
        CompDO compDO = compDOMapper.selectOne(queryWrapper);
        return domainTunnel.getByAggregateId(Comp.class, compDO.getCompId());
    }

    public GridLimit priceLimit(UnitType unitType) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        if (unitType == UnitType.GENERATOR) {
            return GridLimit.builder().low(marketSettingDO.getBidPriceFloor()).high(marketSettingDO.getBidPriceCap()).build();
        } else if (unitType == UnitType.LOAD) {
            return GridLimit.builder().low(marketSettingDO.getOfferPriceFloor()).high(marketSettingDO.getOfferPriceCap()).build();
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
    }


    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        MetaUnit to(MetaUnitDO metaUnitDO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Bid to(BidDO bidDO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        BidDO to(Bid bid);

    }

}
