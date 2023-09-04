package com.bilanee.octopus.demo;

import com.bilanee.octopus.adapter.ws.WsHandler;
import com.bilanee.octopus.adapter.ws.WsMessage;
import com.bilanee.octopus.adapter.ws.WsTopic;
import com.bilanee.octopus.basic.TokenUtils;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.common.tool.util.Json;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoFacade {


    @GetMapping("/centralizedClearance")
    public Result<CentralizedClearance> centralizedClearance() {
        Section sellSection0 = Section.builder().unitId(1L).lx(0D).rx(100D).y(100D).build();
        Section sellSection1 = Section.builder().unitId(2L).lx(100D).rx(300D).y(250D).build();
        Section sellSection2 = Section.builder().unitId(3L).lx(300D).rx(350D).y(300D).build();
        Section sellSection3 = Section.builder().unitId(1L).lx(350D).rx(500D).y(400D).build();

        Section buySection0 = Section.builder().unitId(4L).lx(0D).rx(50D).y(800D).build();
        Section buySection1 = Section.builder().unitId(5L).lx(50D).rx(190D).y(4500D).build();
        Section buySection2 = Section.builder().unitId(6L).lx(190D).rx(400D).y(200D).build();
        Section buySection3 = Section.builder().unitId(4L).lx(400D).rx(500D).y(80D).build();

        UnitVO unitVO1 = UnitVO.builder().unitId(1L).unitName("机组1").build();
        UnitVO unitVO2 = UnitVO.builder().unitId(2L).unitName("机组2").build();
        UnitVO unitVO3 = UnitVO.builder().unitId(3L).unitName("机组3").build();

        UnitVO unitVO4 = UnitVO.builder().unitId(4L).unitName("负荷1").build();
        UnitVO unitVO5 = UnitVO.builder().unitId(5L).unitName("负荷2").build();
        UnitVO unitVO6 = UnitVO.builder().unitId(6L).unitName("负荷3").build();

        CentralizedClearance centralizedClearance = CentralizedClearance.builder()
                .buyDeclaredQuantity(1000D)
                .sellDeclaredQuantity(2000D)
                .dealQuantity(1000D)
                .dealPrice(1000D)
                .buySections(Collect.asList(buySection0, buySection1, buySection2, buySection3))
                .sellSections(Collect.asList(sellSection0, sellSection1, sellSection2, sellSection3))
                .unitVOs(Collect.asList(unitVO1, unitVO2, unitVO3, unitVO4, unitVO5, unitVO6))
                .build();
        return Result.success(centralizedClearance);
    }


    @GetMapping("trigger")
    public Result<Void> trigger(WsTopic wsTopic, String messageEntity, @RequestHeader String token) {
        WsMessage wsMessage = WsMessage.builder().wsTopic(wsTopic).entity(messageEntity).build();
        WsHandler.push(TokenUtils.getUserId(token), Json.toJson(wsMessage));
        return Result.success();
    }

}
