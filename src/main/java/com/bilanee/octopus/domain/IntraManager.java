package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.infrastructure.entity.BidDO;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IntraManager implements ApplicationRunner {

    Map<IntraSymbol, IntraProcessor> intraProcessors = new HashMap<>();
    final BidDOMapper bidDOMapper;
    final Tunnel tunnel;

    public void declare(Bid bid) {
        IntraSymbol intraSymbol = new IntraSymbol(bid.getProvince(), bid.getTimeFrame());
        IntraProcessor intraProcessor = intraProcessors.get(intraSymbol);
        intraProcessor.declare(bid);
    }

    public void cancel(Long bidId) {
        BidDO bidDO = bidDOMapper.selectById(bidId);
        IntraSymbol intraSymbol = new IntraSymbol(bidDO.getProvince(), bidDO.getTimeFrame());
        IntraProcessor intraProcessor = intraProcessors.get(intraSymbol);
        intraProcessor.cancel(bidId);
    }

    public void cancelAll() {
        intraProcessors.values().forEach(IntraProcessor::cancelAll);
    }

    @Override
    public void run(ApplicationArguments args) {

        for (Province province : Province.values()) {
            for (TimeFrame timeFrame : TimeFrame.values()) {
                IntraProcessor intraProcessor = new IntraProcessor(tunnel);
                intraProcessors.put(new IntraSymbol(province, timeFrame), intraProcessor);
            }
        }

    }
}
