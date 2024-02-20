package com.bilanee.octopus;

import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.vo.IntraSymbolBidVO;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.domain.CompEvent;
import com.bilanee.octopus.domain.Routers;
import com.stellariver.milky.common.base.BeanUtil;
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
    @Test
    @SuppressWarnings("UnstableApiUsage")
    public void interPointTest() {
        Ssh.exec("python manage.py intra_spot_default_bid");
        Ssh.exec("python manage.py forward_default_bid");}

}
