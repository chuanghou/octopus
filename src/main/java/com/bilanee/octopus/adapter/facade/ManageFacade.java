package com.bilanee.octopus.adapter.facade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.po.*;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.facade.vo.UserVO;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.GridLimit;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.*;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.common.tool.util.StreamMap;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    final IntraManager intraManager;
    final UnitBasicMapper unitBasicMapper;
    final LoadBasicMapper loadBasicMapper;
    final MarketSettingMapper marketSettingMapper;
    final MetaUnitDOMapper metaUnitDOMapper;
    final MinOutputCostDOMapper minOutputCostDOMapper;
    final BidDOMapper bidDOMapper;
    final ClearanceDOMapper clearanceDOMapper;
    final IntraInstantDOMapper intraInstantDOMapper;
    final IntraQuotationDOMapper intraQuotationDOMapper;
    final UnitDOMapper unitDOMapper;
    /**
     * 获取所有用户信息
     */
    @GetMapping("listUserVOs")
    public List<UserVO> listUserVOs() {
        List<UserDO> userDOs = userDOMapper.selectList(null);
        return Collect.transfer(userDOs, userDO -> new UserVO(userDO.getUserId(), userDO.getUserName(), userDO.getPortrait(), userDO.getPassword()));
    }




    /**
     * 管理员页面RunningVO接口，当data为null时候，表示当前没有竞赛
     */
    @GetMapping("runningCompVO")
    public Result<CompVO> runningCompVO() {
        Comp comp = tunnel.runningComp();
        if (comp == null || comp.getCompStage() == CompStage.END) {
            return Result.success(null);
        }
        return Result.success(CompFacade.Convertor.INST.to(comp));
    }

    /**
     * 管理员页面查看机组负荷分配结果
     */
    @GetMapping("listUnits")
    @SuppressWarnings("unchecked")
    public Result<List<Unit>> listUnits() {
        CompVO compVO = runningCompVO().getData();
        if (compVO == null) {
            return Result.success(Collections.EMPTY_LIST);
        }
        List<Unit> units = tunnel.listUnits(compVO.getCompId(), null, null);
        return Result.success(units);
    }


    @SneakyThrows
    @PostMapping("/createComp")
    public Result<Void> createComp(@RequestBody CompCreatePO compCreatePO) {
        intraManager.clear();

        Ssh.exec("python manage.py empty_data");

        bidDOMapper.selectList(null).forEach(c -> bidDOMapper.deleteById(c.getBidId()));
        compDOMapper.selectList(null).forEach(c -> compDOMapper.deleteById(c.getCompId()));
        clearanceDOMapper.selectList(null).forEach(c -> clearanceDOMapper.deleteById(c.getId()));
        intraInstantDOMapper.selectList(null).forEach(c -> intraInstantDOMapper.deleteById(c.getId()));
        intraQuotationDOMapper.selectList(null).forEach(c -> intraQuotationDOMapper.deleteById(c.getId()));
        unitDOMapper.selectList(null).forEach(c -> unitDOMapper.deleteById(c.getUnitId()));


        Ssh.exec("python manage.py init_data");
        Ssh.exec("python manage.py intra_spot_default_bid");
        Ssh.exec("python manage.py forward_default_bid");

        List<GeneratorBasic> generatorBasics = unitBasicMapper.selectList(null);
        List<IndividualLoadBasic> individualLoadBasics = loadBasicMapper.selectList(null);
        Map<Integer, Double> minOutPuts = minOutputCostDOMapper.selectList(null).stream()
                .collect(Collectors.toMap(MinOutputCostDO::getUnitId, MinOutputCostDO::getSpotCostMinoutput));
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        for (MetaUnitDO metaUnitDO : metaUnitDOMapper.selectList(null)) {
            LambdaQueryWrapper<MetaUnitDO> eq = new LambdaQueryWrapper<MetaUnitDO>().eq(MetaUnitDO::getSourceId, metaUnitDO.getSourceId());
            metaUnitDOMapper.delete(eq);
        }

        generatorBasics.forEach(g -> {
            Double maxForwardUnitOpenInterest = marketSettingDO.getMaxForwardUnitPositionInterest();
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
                    .maxCapacity(g.getMaxP())
                    .minCapacity(g.getMinP())
                    .minOutputPrice(minOutPuts.get(g.getUnitId()))
                    .build();
            metaUnitDOMapper.insert(metaUnitDO);
        });

        individualLoadBasics.forEach(i -> {
            Double maxForwardLoadOpenInterest = marketSettingDO.getMaxForwardLoadPositionInterest();
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


        Map<TradeStage, Integer> bidLengths = StreamMap.<TradeStage, Integer>init()
                .put(TradeStage.AN_INTER, marketSettingDO.getInterprovincialAnnualBidDuration())
                .put(TradeStage.AN_INTRA, marketSettingDO.getIntraprovincialAnnualBidDuration())
                .put(TradeStage.MO_INTER, marketSettingDO.getInterprovincialMonthlyBidDuration())
                .put(TradeStage.MO_INTRA, marketSettingDO.getIntraprovincialMonthlyBidDuration())
                .put(TradeStage.DA_INTRA, marketSettingDO.getIntraprovincialSpotBidDuration())
                .put(TradeStage.DA_INTER, marketSettingDO.getInterprovincialSpotBidDuration())
                .getMap();

        Map<TradeStage, Integer> resultLengths = StreamMap.<TradeStage, Integer>init()
                .put(TradeStage.AN_INTER, marketSettingDO.getInterprovincialAnnualResultDuration())
                .put(TradeStage.AN_INTRA, marketSettingDO.getIntraprovincialAnnualResultDuration())
                .put(TradeStage.MO_INTER, marketSettingDO.getInterprovincialMonthlyResultDuration())
                .put(TradeStage.MO_INTRA, marketSettingDO.getIntraprovincialMonthlyResultDuration())
                .put(TradeStage.DA_INTRA, marketSettingDO.getIntraprovincialSpotResultDuration())
                .put(TradeStage.DA_INTER, marketSettingDO.getInterprovincialSpotResultDuration())
                .getMap();

        DelayConfig delayConfig = DelayConfig.builder()
                .marketStageBidLengths(bidLengths)
                .marketStageClearLengths(resultLengths)
                .quitCompeteLength(marketSettingDO.getQuizCompeteDuration())
                .quitResultLength(marketSettingDO.getQuizResultDuration())
                .tradeResultLength(marketSettingDO.getSettleResultDuration())
                .rankingLength(marketSettingDO.getReviewDuration())
                .build();

        Integer traderNum = marketSettingDO.getTraderNum();
        List<String> userIds = userDOMapper.selectList(null).subList(0, traderNum).stream().map(UserDO::getUserId).collect(Collectors.toList());
        CompCmd.Create command = CompCmd.Create.builder()
                .compId(uniqueIdGetter.get())
                .userIds(userIds)
                .startTimeStamp(compCreatePO.getStartTimeStamp() == null ? Clock.currentTimeMillis() + 30_000L : compCreatePO.getStartTimeStamp())
                .delayConfig(delayConfig)
                .enableQuiz(marketSettingDO.getIsConductingQAndAModule())
                .dt(marketSettingDO.getDt())
                .build();

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
        } else if (comp.getCompStage() == CompStage.END) {
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


    /**
     * 查看算例参数
     */
    @GetMapping("getExampleSetting")
    public Result<ExampleSetting> getExampleSetting() {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        String caseSetting = marketSettingDO.getCaseSetting();
        ExampleSetting.ExampleSettingBuilder builder = ExampleSetting.builder();
        if (Kit.notBlank(caseSetting)) {
            char[] charArray = caseSetting.toCharArray();
            builder.transferDiffer(Objects.equals(charArray[0], '1'));
            builder.transferLoadPeak(Objects.equals(charArray[1], '1'));
            builder.receiverDiffer(Objects.equals(charArray[2], '1'));
            builder.receiverLoadPeak(Objects.equals(charArray[3], '1'));
        }

        String forecastDeviation = marketSettingDO.getForecastDeviation();
        if (Kit.notBlank(forecastDeviation)) {
            String[] split = StringUtils.split(forecastDeviation, ":");

            ForecastError loadAnnual = ForecastError.builder().forecastError(marketSettingDO.getLoadAnnualMaxForecastErr())
                    .transfer(Double.valueOf(split[0])).receiver(Double.valueOf(split[3])).build();
            builder.loadAnnualMaxForecastErr(loadAnnual);

            ForecastError loadMonthly = ForecastError.builder().forecastError(marketSettingDO.getLoadMonthlyMaxForecastErr())
                    .transfer(Double.valueOf(split[1])).receiver(Double.valueOf(split[4])).build();
            builder.loadMonthlyMaxForecastErr(loadMonthly);

            ForecastError loadDa = ForecastError.builder().forecastError(marketSettingDO.getLoadDaMaxForecastErr())
                    .transfer(Double.valueOf(split[2])).receiver(Double.valueOf(split[5])).build();
            builder.loadDaMaxForecastErr(loadDa);

            ForecastError generatorAnnual = ForecastError.builder().forecastError(marketSettingDO.getLoadAnnualMaxForecastErr())
                    .transfer(Double.valueOf(split[6])).receiver(Double.valueOf(split[9])).build();
            builder.renewableAnnualMaxForecastErr(generatorAnnual);

            ForecastError generatorMonthly = ForecastError.builder().forecastError(marketSettingDO.getLoadMonthlyMaxForecastErr())
                    .transfer(Double.valueOf(split[7])).receiver(Double.valueOf(split[10])).build();
            builder.renewableMonthlyMaxForecastErr(generatorMonthly);

            ForecastError generatorDa = ForecastError.builder().forecastError(marketSettingDO.getLoadDaMaxForecastErr())
                    .transfer(Double.valueOf(split[8])).receiver(Double.valueOf(split[11])).build();
            builder.renewableDaMaxForecastErr(generatorDa);

        }
        ExampleSetting exampleSetting = builder.build();
        return Result.success(exampleSetting);
    }


    /**
     * 更新算例参数
     */
    @GetMapping("updateExampleSetting")
    public Result<Void> updateExampleSetting(@RequestBody ExampleSetting exampleSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        String caseSetting = (exampleSetting.getTransferDiffer() ? "1" : "0") +
                (exampleSetting.getTransferLoadPeak() ? "1" : "0") +
                (exampleSetting.getReceiverDiffer() ? "1" : "0") +
                (exampleSetting.getReceiverLoadPeak() ? "1" : "0");
        marketSettingDO.setCaseSetting(caseSetting);

        marketSettingDO.setLoadAnnualMaxForecastErr(exampleSetting.getLoadAnnualMaxForecastErr().getForecastError());
        marketSettingDO.setLoadMonthlyMaxForecastErr(exampleSetting.getLoadMonthlyMaxForecastErr().getForecastError());
        marketSettingDO.setLoadDaMaxForecastErr(exampleSetting.getLoadDaMaxForecastErr().getForecastError());
        marketSettingDO.setRenewableAnnualMaxForecastErr(exampleSetting.getRenewableAnnualMaxForecastErr().getForecastError());
        marketSettingDO.setRenewableMonthlyMaxForecastErr(exampleSetting.getRenewableMonthlyMaxForecastErr().getForecastError());
        marketSettingDO.setRenewableDaMaxForecastErr(exampleSetting.getRenewableDaMaxForecastErr().getForecastError());
        String forecastDeviation = Stream.of(exampleSetting.getLoadAnnualMaxForecastErr().getTransfer(),
                exampleSetting.getLoadMonthlyMaxForecastErr().getTransfer(),
                exampleSetting.getLoadDaMaxForecastErr().getTransfer(),

                exampleSetting.getLoadAnnualMaxForecastErr().getReceiver(),
                exampleSetting.getLoadMonthlyMaxForecastErr().getReceiver(),
                exampleSetting.getLoadDaMaxForecastErr().getReceiver(),

                exampleSetting.getRenewableAnnualMaxForecastErr().getTransfer(),
                exampleSetting.getRenewableMonthlyMaxForecastErr().getTransfer(),
                exampleSetting.getRenewableDaMaxForecastErr().getTransfer(),

                exampleSetting.getRenewableAnnualMaxForecastErr().getReceiver(),
                exampleSetting.getRenewableMonthlyMaxForecastErr().getReceiver(),
                exampleSetting.getRenewableDaMaxForecastErr().getReceiver()).map(Objects::toString).collect(Collectors.joining(":"));
        marketSettingDO.setForecastDeviation(forecastDeviation);
        marketSettingMapper.updateById(marketSettingDO);
        return Result.success();
    }



    /**
     * 查看知识竞答参数
     */
    @GetMapping("getQuizSetting")
    public Result<QuizSetting> getQuizSetting() {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        QuizSetting quizSetting = QuizSetting.builder().quizId(marketSettingDO.getPaperId())
                .enableQuiz(marketSettingDO.getIsConductingQAndAModule())
                .quizCompeteDuration(marketSettingDO.getQuizCompeteDuration())
                .quizResultDuration(marketSettingDO.getQuizResultDuration())
                .build();
        return Result.success(quizSetting);
    }


    /**
     * 修改知识竞答参数
     */
    @GetMapping("updateQuizSetting")
    public Result<Void> updateQuizSetting(@RequestBody QuizSetting quizSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        marketSettingDO.setIsConductingQAndAModule(quizSetting.getEnableQuiz());
        marketSettingDO.setPaperId(quizSetting.getQuizId());
        marketSettingDO.setQuizCompeteDuration(quizSetting.getQuizCompeteDuration());
        marketSettingDO.setQuizResultDuration(quizSetting.getQuizResultDuration());
        marketSettingMapper.updateById(marketSettingDO);
        return Result.success();

    }


    /**
     * 查看电力市场参数设定
     */
    @GetMapping("getElectricMarketSetting")
    public Result<ElectricMarketSettingVO> getElectricMarketSetting() {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        ElectricMarketSettingVO electricMarketSettingVO = ElectricMarketSettingVO.builder()
                .generatorPriceLimit(GridLimit.builder().high(marketSettingDO.getOfferPriceCap()).low(marketSettingDO.getOfferPriceFloor()).build())
                .loadPriceLimit(GridLimit.builder().high(marketSettingDO.getBidPriceCap()).low(marketSettingDO.getBidPriceFloor()).build())
                .transmissionAndDistributionTariff(marketSettingDO.getTransmissionAndDistributionTariff())
                .coalPriceMultiple(marketSettingDO.getCoalPriceMultiple())
                .maxForwardUnitPositionInterest(marketSettingDO.getMaxForwardUnitPositionInterest())
                .maxForwardLoadPositionInterest(marketSettingDO.getMaxForwardLoadPositionInterest())
                .regulatedProducerPrice(marketSettingDO.getRegulatedProducerPrice())
                .regulatedInterprovTransmissionPrice(marketSettingDO.getRegulatedInterprovTransmissionPrice())
                .regulatedUserTariff(marketSettingDO.getRegulatedUserTariff())
                .forwardNumBidSegs(marketSettingDO.getForwardNumBidSegs())
                .forwardNumOfferSegs(marketSettingDO.getForwardNumBidSegs())
                .spotNumBidSegs(marketSettingDO.getSpotNumBidSegs())
                .spotNumOfferSegs(marketSettingDO.getSpotNumOfferSegs())
                .interprovClearingMode(clearingMode.get(marketSettingDO.getInterprovClearingMode()))
                .interprovTradingMode(tradingMode.get(marketSettingDO.getInterprovTradingMode()))
                .build();
        return Result.success(electricMarketSettingVO);
    }


    static final private Map<Integer, String> clearingMode = Collect.asMap(
            1, "政府定价定量",
            2, "政府定量不定价",
            3, "政府干预量不定价"
    );

    static final private Map<Integer, String> tradingMode = Collect.asMap(
            1, "边际统一出清",
            2, "按匹配对分别出清"
    );


    /**
     * 修改电力市场参数设定
     */
    @GetMapping("updateElectricMarketSetting")
    public Result<Void> updateElectricMarketSetting(@RequestBody ElectricMarketSetting electricMarketSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        marketSettingDO.setOfferPriceCap(electricMarketSetting.getGeneratorPriceLimit().getHigh());
        marketSettingDO.setOfferPriceFloor(electricMarketSetting.getGeneratorPriceLimit().getLow());
        marketSettingDO.setBidPriceCap(electricMarketSetting.getLoadPriceLimit().getHigh());
        marketSettingDO.setBidPriceFloor(electricMarketSetting.getLoadPriceLimit().getLow());
        marketSettingDO.setTransmissionAndDistributionTariff(electricMarketSetting.getTransmissionAndDistributionTariff());
        marketSettingDO.setCoalPriceMultiple(electricMarketSetting.getCoalPriceMultiple());
        marketSettingDO.setMaxForwardLoadPositionInterest(electricMarketSetting.getMaxForwardLoadPositionInterest());
        marketSettingDO.setMaxForwardUnitPositionInterest(electricMarketSetting.getMaxForwardUnitPositionInterest());
        marketSettingDO.setRegulatedProducerPrice(electricMarketSetting.getRegulatedProducerPrice());
        marketSettingDO.setRegulatedUserTariff(electricMarketSetting.getRegulatedUserTariff());
        marketSettingDO.setRegulatedInterprovTransmissionPrice(electricMarketSetting.getRegulatedInterprovTransmissionPrice());
        marketSettingMapper.updateById(marketSettingDO);
        return Result.success();
    }


    /**
     * 获取竞赛仿真参数
     */
    @GetMapping("getSimulateSetting")
    public Result<SimulateSetting> getSimulateSetting() {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        SimulateSetting simulateSetting = Convertor.INST.to(marketSettingDO);
        return Result.success(simulateSetting);
    }


    /**
     * 更新竞赛仿真参数
     */
    @PostMapping("updateSimulateSetting")
    public Result<Void> updateSimulateSetting(@RequestBody SimulateSetting simulateSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(2);
        marketSettingDO.setIntraprovincialAnnualBidDuration(simulateSetting.getIntraprovincialAnnualBidDuration());
        marketSettingDO.setIntraprovincialMonthlyBidDuration(simulateSetting.getIntraprovincialMonthlyBidDuration());
        marketSettingDO.setIntraprovincialSpotBidDuration(simulateSetting.getIntraprovincialSpotBidDuration());
        marketSettingDO.setInterprovincialAnnualBidDuration(simulateSetting.getInterprovincialAnnualBidDuration());
        marketSettingDO.setInterprovincialMonthlyBidDuration(simulateSetting.getInterprovincialMonthlyBidDuration());
        marketSettingDO.setInterprovincialSpotBidDuration(simulateSetting.getInterprovincialSpotBidDuration());
        marketSettingDO.setInterprovincialAnnualResultDuration(simulateSetting.getInterprovincialAnnualResultDuration());
        marketSettingDO.setIntraprovincialAnnualResultDuration(simulateSetting.getIntraprovincialAnnualResultDuration());
        marketSettingDO.setInterprovincialMonthlyResultDuration(simulateSetting.getInterprovincialMonthlyResultDuration());
        marketSettingDO.setIntraprovincialMonthlyResultDuration(simulateSetting.getIntraprovincialMonthlyResultDuration());
        marketSettingDO.setInterprovincialSpotResultDuration(simulateSetting.getInterprovincialSpotResultDuration());
        marketSettingDO.setIntraprovincialSpotResultDuration(simulateSetting.getIntraprovincialSpotResultDuration());
        marketSettingDO.setSettleResultDuration(simulateSetting.getSettleResultDuration());
        marketSettingDO.setReviewDuration(simulateSetting.getReviewDuration());
        marketSettingDO.setRobotNum(simulateSetting.getRobotNum());
        marketSettingDO.setTraderNum(simulateSetting.getTraderNum());
        marketSettingDO.setIsSettingDefaultOfferForTraders(simulateSetting.getIsSettingDefaultOfferForTraders());
        marketSettingDO.setIsSettingDefaultOfferForRobots(simulateSetting.getIsSettingDefaultOfferForRobots());
        marketSettingDO.setIsEnteringReviewStage(simulateSetting.getIsEnteringReviewStage());
        marketSettingMapper.updateById(marketSettingDO);
        return Result.success();
    }



    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompVO to(Comp comp);


        @BeanMapping(builder = @Builder(disableBuilder = true))
        default String toString(StageId stageId) {
            return stageId.toString();
        }

        @BeanMapping(builder = @Builder(disableBuilder = true))
        SimulateSetting to(MarketSettingDO marketSettingDO);

    }

}
