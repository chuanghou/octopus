package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.facade.po.CompCreatePO;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.facade.vo.UserVO;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.CompCmd;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.mapstruct.Mapping;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理页面
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/manage")
public class ManageFacade {

    final UniqueIdGetter uniqueIdGetter;
    final DomainTunnel domainTunnel;
    final CompDOMapper compDOMapper;
    final UserDOMapper userDOMapper;
    final Tunnel tunnel;
    final Comp.DelayExecutor delayExecutor;

    /**
     * 获取所有用户信息
     */
    @GetMapping("listUserVOs")
    public List<UserVO> listUserVOs() {
        List<UserDO> userDOs = userDOMapper.selectList(null);
        return Collect.transfer(userDOs, userDO -> new UserVO(userDO.getUserId(), userDO.getUserName(), userDO.getPortrait()));
    }

    final UnitBasicMapper unitBasicMapper;
    final LoadBasicMapper loadBasicMapper;
    final MinOutputCostMapper minOutputCostMapper;
    final MarketSettingMapper marketSettingMapper;
    final MetaUnitDOMapper metaUnitDOMapper;
    /**
     * 竞赛新建接口
     * @param compCreatePO 创建竞赛参数
     */
    @PostMapping("/createComp")
    public Result<Void> createComp(@RequestBody CompCreatePO compCreatePO) {
        // TODO 根据参数初始化数据库的marketSetting
        // TODO Ssh.exec(""); 初始化脚本

        List<GeneratorBasic> generatorBasics = unitBasicMapper.selectList(null);
        List<IndividualLoadBasic> individualLoadBasics = loadBasicMapper.selectList(null);
        Map<Integer, Double> minOutPuts = minOutputCostMapper.selectList(null).stream().collect(Collectors.toMap(MinOutputCost::getUnitId, MinOutputCost::getSpotCostMinoutput));
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        metaUnitDOMapper.delete(null);
        generatorBasics.forEach(g -> {
            Double maxForwardUnitOpenInterest = marketSettingDO.getMaxForwardUnitOpenInterest();
            Double maxP = g.getMaxP();
            maxP = maxP * maxForwardUnitOpenInterest;
            Map<Direction, Double> map = Collect.asMap(Direction.BUY, 0D, Direction.SELL, maxP);
            Map<TimeFrame, Map<Direction, Double>> capacity = Collect.asMap(TimeFrame.PEAK, map, TimeFrame.FLAT, map, TimeFrame.VALLEY, map);
            MetaUnitDO metaUnitDO = MetaUnitDO.builder()
                    .name(g.getUnitName())
                    .province(Kit.enumOfMightEx(Province::getDbCode, g.getProv()))
                    .unitType(UnitType.GENERATOR)
                    .generatorType(Kit.enumOfMightEx(GeneratorType::getDbCode, g.getType()))
                    .sourceId(g.getUnitId())
                    .capacity(capacity)
                    .maxCapacity(g.getMinP())
                    .minOutputPrice(minOutPuts.get(g.getUnitId()))
                    .build();
            metaUnitDOMapper.insert(metaUnitDO);
        });

        individualLoadBasics.forEach(i -> {
            Double maxForwardLoadOpenInterest = marketSettingDO.getMaxForwardLoadOpenInterest();
            Double maxP = i.getMaxP();
            maxP = maxP * maxForwardLoadOpenInterest;
            Map<Direction, Double> map = Collect.asMap(Direction.SELL, 0D, Direction.BUY, maxP);
            Map<TimeFrame, Map<Direction, Double>> capacity = Collect.asMap(TimeFrame.PEAK, map, TimeFrame.FLAT, map, TimeFrame.VALLEY, map);
            MetaUnitDO metaUnitDO = MetaUnitDO.builder().name(i.getLoadName())
                    .province(Kit.enumOfMightEx(Province::getDbCode, i.getProv()))
                    .unitType(UnitType.LOAD)
                    .sourceId(i.getLoadId())
                    .capacity(capacity)
                    .maxCapacity(i.getMaxP())
                    .minCapacity(0D)
                    .minOutputPrice(null)
                    .build();
            metaUnitDOMapper.insert(metaUnitDO);

        });


        delayExecutor.removeStepCommand();
        CompCmd.Create command = Convertor.INST.to(compCreatePO);
        String dt = DateFormatUtils.format(marketSettingDO.getDt(), "yyyyMMdd");
        command.setDt(dt);
        if (command.getStartTimeStamp() == null) {
            command.setStartTimeStamp(Clock.currentTimeMillis() + 30 * 1000L);
        }
        command.setCompId(uniqueIdGetter.get());
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
    }

    /**
     * 手动下一阶段
     */
    @PostMapping("/step")
    public Result<Void> step() {
        Comp comp = tunnel.runningComp();
        if (comp == null) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("没有运行中的竞赛"));
        } else if (comp.getCompStage() == CompStage.RANKING) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("已经到了最后阶段"));
        }
        delayExecutor.removeStepCommand();
        CompCmd.Step command = CompCmd.Step.builder().stageId(comp.getStageId().next(comp)).build();
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
    }

    /**
     * 手动多个阶段
     */
    @PostMapping("/steps")
    public Result<Void> steps(@RequestParam Integer steps) {
        for (int i = 0; i < steps; i++) {
            step();
        }
        return Result.success();
    }




    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        @Mapping(source = "quitCompeteLength", target = "delayConfig.quitCompeteLength")
        @Mapping(source = "quitResultLength", target = "delayConfig.quitResultLength")
        @Mapping(source = "marketStageBidLengths", target = "delayConfig.marketStageBidLengths")
        @Mapping(source = "marketStageClearLengths", target = "delayConfig.marketStageClearLengths")
        @Mapping(source = "tradeResultLength", target = "delayConfig.tradeResultLength")
        CompCmd.Create to(CompCreatePO compCreatePO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompVO to(Comp comp);


        @BeanMapping(builder = @Builder(disableBuilder = true))
        default String toString(StageId stageId) {
            return stageId.toString();
        }

    }

}
