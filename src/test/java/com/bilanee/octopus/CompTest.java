package com.bilanee.octopus;

import com.bilanee.octopus.adapter.CompCreatePO;
import com.bilanee.octopus.adapter.CompFacade;
import com.bilanee.octopus.adapter.ManageFacade;
import com.bilanee.octopus.basic.TradeStage;
import com.bilanee.octopus.infrastructure.entity.MetaUnitDO;
import com.bilanee.octopus.infrastructure.mapper.MetaUnitDOMapper;
import com.stellariver.milky.common.tool.util.Json;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
    MetaUnitDOMapper metaUnitDOMapper;
    @Test
    public void testDelay() throws InterruptedException {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 1);
            marketStageClearLengths.put(marketStage, 1);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .compInitLength(1)
                .quitCompeteLength(1)
                .quitResultLength(1)
                .marketStageBidLengths(marketStageBidLengths)
                .marketStageClearLengths(marketStageClearLengths)
                .tradeResultLength(1)
                .build();

        System.out.println(Json.toJson(compCreatePO));
        manageFacade.createComp(compCreatePO);
        Thread.sleep(60_000);
        System.out.println("test");
    }

    @Test
    public void testMetaUnit() {
        List<MetaUnitDO> metaUnitDOS = metaUnitDOMapper.selectList(null);
        System.out.println(metaUnitDOS);
    }
}
