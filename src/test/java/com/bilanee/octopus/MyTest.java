package com.bilanee.octopus;

import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.QuizFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.BidPO;
import com.bilanee.octopus.adapter.facade.po.RollBidPO;
import com.bilanee.octopus.adapter.facade.vo.RollSymbolBidVO;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.Routers;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.stellariver.milky.common.base.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
        Comp comp = tunnel.runningComp();
        Result<List<RollSymbolBidVO>> listResult = unitFacade.listRollSymbolBidVOs(comp.getStageId().toString(), TokenUtils.sign("1000"));
        System.out.println(listResult);
    }
}
