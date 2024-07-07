package com.bilanee.octopus;

import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.QuizFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.BidPO;
import com.bilanee.octopus.adapter.facade.po.RollBidPO;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.domain.Routers;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
    public void interPointTest() throws InterruptedException {
        BidPO bidPO = BidPO.builder().direction(Direction.SELL).unitId(756191L).quantity(10D).price(10D).instant(0).build();
        RollBidPO rollBidPO = RollBidPO.builder().stageId("756158.TRADE.0.ROLL.BID").bidPO(bidPO).build();
        unitFacade.submitRollBidPO(rollBidPO);
        Thread.sleep(100000);
    }
}
