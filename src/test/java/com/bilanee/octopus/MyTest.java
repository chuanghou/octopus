package com.bilanee.octopus;

import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.po.ElectricMarketSettingVO;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.domain.ClearUtil;
import com.bilanee.octopus.domain.CompEvent;
import com.bilanee.octopus.domain.Routers;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MyTest {

    @Autowired
    ManageFacade manageFacade;

    @Test
    @SuppressWarnings("UnstableApiUsage")
    public void interPointTest() {
        Routers routers = BeanUtil.getBean(Routers.class);
        CompEvent.Stepped stepped = CompEvent.Stepped.builder().build();
        stepped.setNow(StageId.builder().roundId(0).build());
        routers.routerAfterInterSpotBid(stepped, null);
    }

}
