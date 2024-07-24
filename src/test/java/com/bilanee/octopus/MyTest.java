package com.bilanee.octopus;

import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.QuizFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.BidPO;
import com.bilanee.octopus.adapter.facade.po.RollBidPO;
import com.bilanee.octopus.adapter.facade.vo.RollSymbolBidVO;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.Routers;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.domain.UnitCmd;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.command.CommandBus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ActiveProfiles("test")
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
        Ssh.exec("pwd");
    }
}
