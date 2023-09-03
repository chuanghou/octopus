package com.bilanee.octopus.adapter.facade;


import com.bilanee.octopus.adapter.facade.po.BidPO;
import com.bilanee.octopus.adapter.facade.po.CentralizedBidsPO;
import com.bilanee.octopus.adapter.facade.po.RealtimeBidPO;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.domain.UnitCmd;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/unit")
public class UnitFacade {

    final UnitDOMapper unitDOMapper;
    final CompFacade compFacade;


    @GetMapping("listCentralizedBidVOs")
    public Result<List<CentralizedBidVO>> listCentralizedBidVOs(String stageId, @RequestHeader String token) {

        return null;
    }


    @PostMapping("submitCentralizedBidsPO")
    public Result<Void> submitCentralizedBidsPO(CentralizedBidsPO centralizedBidsPO) {
        StageId stageId = StageId.parse(centralizedBidsPO.getStageId());
        List<BidPO> bidPOs = centralizedBidsPO.getBidPOs();
        UnitCmd.CentralizedBids command = UnitCmd.CentralizedBids.builder().stageId(stageId)
                .bids(Collect.transfer(bidPOs, Convertor.INST::to))
                .build();
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
    }

    @PostMapping("submitRealtimeBidPO")
    public Result<Void> submitRealtimeBidPO(RealtimeBidPO realtimeBidPO) {

        return null;
    }

    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Bid to(BidPO bidPO);

    }

}
