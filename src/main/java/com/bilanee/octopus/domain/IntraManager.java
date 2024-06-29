package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.TradeType;
import com.bilanee.octopus.infrastructure.entity.BidDO;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IntraManager implements ApplicationRunner {

    Map<Object, Processor> processors = new HashMap<>();
    final BidDOMapper bidDOMapper;
    final Tunnel tunnel;
    final UniqueIdGetter uniqueIdGetter;

    public void declare(Bid bid) {
        Object symbol;
        if (bid.getTradeStage().getTradeType() == TradeType.INTRA) {
            symbol = new IntraSymbol(bid.getProvince(), bid.getTimeFrame());
        } else if (bid.getTradeStage().getTradeType() == TradeType.ROLL) {
            symbol = new RollSymbol(bid.getProvince(), bid.getInstant());
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
        Processor processor = processors.get(symbol);
        processor.declare(bid);
    }

    public void cancel(Long bidId) {
        BidDO bidDO = bidDOMapper.selectById(bidId);
        Object symbol;
        if (bidDO.getTradeStage().getTradeType() == TradeType.INTRA) {
            symbol = new IntraSymbol(bidDO.getProvince(), bidDO.getTimeFrame());
        } else if (bidDO.getTradeStage().getTradeType() == TradeType.ROLL) {
            symbol = new RollSymbol(bidDO.getProvince(), bidDO.getInstant());
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
        Processor processor = processors.get(symbol);
        processor.cancel(bidId, bidDO.getDirection());
    }

    public void close() {
        processors.values().forEach(Processor::close);
    }

    public void clear() {
        processors.values().forEach(Processor::clear);
    }

    @Override
    public void run(ApplicationArguments args) {

        for (Province province : Province.values()) {
            for (TimeFrame timeFrame : TimeFrame.values()) {
                IntraSymbol intraSymbol = new IntraSymbol(province, timeFrame);
                Processor processor = new Processor(tunnel, intraSymbol, uniqueIdGetter);
                processors.put(intraSymbol, processor);
            }
            for (int i = 0; i < 24; i++) {
                RollSymbol rollSymbol = new RollSymbol(province, i);
                Processor processor = new Processor(tunnel, rollSymbol, uniqueIdGetter);
                processors.put(rollSymbol, processor);
            }
        }

    }
}
