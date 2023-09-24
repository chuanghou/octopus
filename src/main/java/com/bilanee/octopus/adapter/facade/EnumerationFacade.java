package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.basic.enums.*;
import com.stellariver.milky.common.base.Enumeration;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 枚举列表
 */

@RestController("/api/enumeration")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnumerationFacade {


    /**
     * 竞赛阶段枚举列表
     */
    @RequestMapping("/compStages")
    public Result<List<Enumeration>> compStages() {
        List<Enumeration> enumerations = Collect
                .transfer(Arrays.asList(CompStage.values()), e -> new Enumeration(e.name(), e.getDesc()));
        return Result.success(enumerations);
    }


    /**
     * 交易阶段枚举列表
     */
    @RequestMapping("/tradeStages")
    public Result<List<Enumeration>> tradeStages() {
        List<Enumeration> enumerations = Collect
                .transfer(Arrays.asList(TradeStage.values()), e -> new Enumeration(e.name(), e.getDesc()));
        return Result.success(enumerations);
    }

    /**
     * 省份枚举列表
     */
    @RequestMapping("/provinces")
    public Result<List<Enumeration>> provinces() {
        List<Enumeration> enumerations = Collect
                .transfer(Arrays.asList(Province.values()),  e -> new Enumeration(e.name(), e.getDesc()));
        return Result.success(enumerations);
    }


    /**
     * 单元类型枚举列表
     */
    @RequestMapping("/unitTypes")
    public Result<List<Enumeration>> unitTypes() {
        List<Enumeration> enumerations = Collect
                .transfer(Arrays.asList(UnitType.values()), e -> new Enumeration(e.name(), e.getDesc()));
        return Result.success(enumerations);
    }


    /**
     * 机组类型枚举列表
     */
    @RequestMapping("/generatorTypes")
    public Result<List<Enumeration>> generatorTypes() {
        List<Enumeration> enumerations = Collect
                .transfer(Arrays.asList(GeneratorType.values()), e -> new Enumeration(e.name(), e.getDesc()));
        return Result.success(enumerations);
    }


    /**
     * 市场状态枚举
     */
    @RequestMapping("/marketStatuses")
    public Result<List<Enumeration>> marketStatuses() {
        List<Enumeration> enumerations = Collect
                .transfer(Arrays.asList(MarketStatus.values()), e -> new Enumeration(e.name(), e.getDesc()));
        return Result.success(enumerations);
    }

}
