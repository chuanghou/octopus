package com.bilanee.octopus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.QuizFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.vo.IntraSymbolBidVO;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.domain.CompCmd;
import com.bilanee.octopus.domain.CompEvent;
import com.bilanee.octopus.domain.Routers;
import com.bilanee.octopus.infrastructure.entity.BidDO;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
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

    @Test
    public void interPointTest() {
        List<String> userIds = new ArrayList<>();
        LambdaQueryWrapper<BidDO> eq = new LambdaQueryWrapper<BidDO>()
                .eq(BidDO::getBidStatus, BidStatus.NEW_DECELERATED)
                        .in(BidDO::getUserId, userIds);
        bidDOMapper.selectList(eq);


    }

}
