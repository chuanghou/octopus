package com.bilanee.octopus;

import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.QuizFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.CompCreatePO;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.ClearUtil;
import com.bilanee.octopus.domain.CompEvent;
import com.bilanee.octopus.domain.Routers;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.domain.support.ErrorEnums;
import com.stellariver.milky.domain.support.invocation.InvokeTrace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MyTest {

    @Autowired
    ManageFacade manageFacade;

    @Autowired
    UnitFacade unitFacade;

    @Autowired
    CompFacade compFacade;

    @Autowired
    QuizFacade quizFacade;

    @Autowired
    BidDOMapper bidDOMapper;

    @Autowired
    Routers routers;

    @Autowired
    Tunnel tunnel;


    @Test
    public void interPointTest() {
        manageFacade.step();
    }


    @SuppressWarnings("UnstableApiUsage")
    private void doClear(List<Bid> bids, TimeFrame timeFrame) {

        bids = bids.stream().filter(bid -> bid.getQuantity() > 0).collect(Collectors.toList());
        List<Bid> sortedBuyBids = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY)
                .sorted(Comparator.comparing(Bid::getPrice).reversed())
                .collect(Collectors.toList());
        List<Bid> sortedSellBids = bids.stream().filter(bid -> bid.getDirection() == Direction.SELL)
                .sorted(Comparator.comparing(Bid::getPrice))
                .collect(Collectors.toList());

        RangeMap<Double, Range<Double>> buyBrokenLine = ClearUtil.buildRangeMap(sortedBuyBids, Double.MAX_VALUE, 0D);
        RangeMap<Double, Range<Double>> sellBrokenLine = ClearUtil.buildRangeMap(sortedSellBids, 0D, Double.MAX_VALUE);

        Point<Double> interPoint = ClearUtil.analyzeInterPoint(buyBrokenLine, sellBrokenLine);

        //  当没有报价的时候，此时相当于交点处于y轴上，因为成交量是0，所以此时成交价格没有意义
        if (interPoint == null) {
            interPoint = new Point<>(0D, null);
        }

        GridLimit transLimit = GridLimit.builder().low(62.23).high(92.79).build();

        double nonMarketQuantity = 0D;
        if (interPoint.x <= transLimit.getLow()) { // 当出清点小于等于最小传输量限制时
            nonMarketQuantity = transLimit.getLow() - interPoint.x;
        }else if (interPoint.x > transLimit.getHigh()) { // // 当出清点大于最大传输量限制时
            interPoint.x = transLimit.getHigh();
            Range<Double> bR = buyBrokenLine.get(interPoint.x);
            Range<Double> sR = sellBrokenLine.get(interPoint.x);
            if (bR == null || sR == null) {
                throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
            }
            interPoint.y = ((bR.upperEndpoint() + bR.lowerEndpoint()) + (sR.upperEndpoint() + sR.lowerEndpoint()))/4;
        }

        if (!Kit.eq(interPoint.x, 0D)) {
            ClearUtil.deal(sortedBuyBids, interPoint, null);
            ClearUtil.deal(sortedSellBids, interPoint, null);
        }

    }

}
