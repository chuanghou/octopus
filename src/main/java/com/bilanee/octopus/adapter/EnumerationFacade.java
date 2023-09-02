package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.TradeStage;
import com.stellariver.milky.common.base.Enumeration;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;


@RestController
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnumerationFacade {



    @RequestMapping("stages")
    public List<Enumeration> stages() {
        return Collect.transfer(Arrays.asList(TradeStage.values()), tradeStage -> new Enumeration(tradeStage.name(), tradeStage.getDesc()));
    }



    
}
