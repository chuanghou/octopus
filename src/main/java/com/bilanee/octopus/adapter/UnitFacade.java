package com.bilanee.octopus.adapter;


import com.bilanee.octopus.basic.CentralizedBidVO;
import com.bilanee.octopus.basic.TradeStage;
import com.bilanee.octopus.basic.UnitVO;
import com.stellariver.milky.common.base.Result;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/unit")
public class UnitFacade {

    @GetMapping("listUnitVOs")
    public List<UnitVO> listUnitVOs() {
        return null;
    }

    @GetMapping("listCentralizedBidVOs")
    public List<CentralizedBidVO> listCentralizedBidVOs(TradeStage tradeStage) {

        return null;
    }


    @PostMapping("submitCentralizedBids")
    public Result<Void> submitCentralizedBids(TradeStage tradeStage) {

        return null;
    }

    @PostMapping("submitRealtimeBid")
    public Result<Void> submitRealtimeBid() {

        return null;
    }


}
