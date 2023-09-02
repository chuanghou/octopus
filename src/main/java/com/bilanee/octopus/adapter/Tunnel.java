package com.bilanee.octopus.adapter;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.infrastructure.entity.BidDO;
import com.bilanee.octopus.infrastructure.entity.MetaUnitDO;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.bilanee.octopus.infrastructure.mapper.MetaUnitDOMapper;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
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

    public Map<String, List<MetaUnit>> assignMetaUnits(Integer roundId, List<String> userIds) {
        Map<String, List<MetaUnit>> metaUnitMap = new HashMap<>();

        for (int i = 0; i < userIds.size(); i++) {
            List<Integer> sourceIds = AllocateUtils.allocateSourceId(roundId, userIds.size(), 30, i);
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

    public void updateBids(List<Bid> bids) {
        List<BidDO> bidDOs = Collect.transfer(bids, Convertor.INST::to);
        bidDOs.forEach(bidDOMapper::updateById);
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
