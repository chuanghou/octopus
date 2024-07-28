package com.bilanee.octopus;

import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.QuizFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.SimulateSetting;
import com.bilanee.octopus.adapter.facade.vo.MultiYearBidVO;
import com.bilanee.octopus.adapter.facade.vo.RetailPackageVO;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.GeneratorType;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.RenewableType;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.domain.Routers;
import com.bilanee.octopus.infrastructure.entity.MarketSettingDO;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.bilanee.octopus.infrastructure.mapper.MarketSettingMapper;
import com.stellariver.milky.common.base.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
    Tunnel tunnel;

    @Autowired
    MarketSettingMapper marketSettingMapper;


    @Test
    public void interPointTest() throws InterruptedException {
        StageId stageId = tunnel.runningComp().getStageId();
        stageId.setTradeStage(TradeStage.MULTI_ANNUAL);
        Result<RetailPackageVO> retailPackageVOResult = unitFacade.listsRetailPackageVO(stageId.toString(), TokenUtils.sign("1001"), Province.TRANSFER);
        Assertions.assertTrue(retailPackageVOResult.getSuccess());
    }
}
