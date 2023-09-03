package com.bilanee.octopus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.po.CompCreatePO;
import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.basic.TradeStage;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
import com.bilanee.octopus.infrastructure.mapper.MetaUnitDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.stellariver.milky.common.base.Result;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompTest {

    @Autowired
    ManageFacade manageFacade;

    @Autowired
    CompFacade compFacade;

    @Autowired
    MetaUnitDOMapper metaUnitDOMapper;

    @Autowired
    UnitDOMapper unitDOMapper;
    @Test
    public void testDelay() throws InterruptedException {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 1);
            marketStageClearLengths.put(marketStage, 1);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .compInitLength(10)
                .quitCompeteLength(1)
                .quitResultLength(1)
                .marketStageBidLengths(marketStageBidLengths)
                .marketStageClearLengths(marketStageClearLengths)
                .tradeResultLength(1)
                .userIds(Arrays.asList("0", "1"))
                .build();

        manageFacade.createComp(compCreatePO);
        Result<CompVO> compVOResult = compFacade.runningComp();
        Assertions.assertTrue(compVOResult.getSuccess());
        Long compId = compVOResult.getData().getCompId();
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, compId);
        List<UnitDO> unitDOS = unitDOMapper.selectList(queryWrapper);
        Assertions.assertEquals(unitDOS.size(), 2 * 3 * 4);
    }

}
