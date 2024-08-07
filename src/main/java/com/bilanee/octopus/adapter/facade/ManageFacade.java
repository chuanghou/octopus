package com.bilanee.octopus.adapter.facade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bilanee.octopus.adapter.facade.po.*;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.facade.vo.PaperVO;
import com.bilanee.octopus.adapter.facade.vo.UserVO;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.*;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.*;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.common.tool.util.StreamMap;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Builder;
import org.mapstruct.Mapping;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 管理页面
 */

@CustomLog
@RestController
@RequiredArgsConstructor
@RequestMapping("/manage")
public class ManageFacade {

    final UniqueIdGetter uniqueIdGetter;
    final DomainTunnel domainTunnel;
    final CompDOMapper compDOMapper;
    final UserDOMapper userDOMapper;
    final Tunnel tunnel;
    final DelayExecutor delayExecutor;
    final ProcessorManager processorManager;
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
    final GameRankingMapper gameRankingMapper;
    final GameResultMapper gameResultMapper;
    final BidAspect bidAspect;


    /**
     * 失效登录令牌
     */
    @PostMapping("resetToken")
    public Result<Void> invalidToken() {
        TokenUtils.TOKEN_SECRET = new Date().toString();
        return Result.success();
    }
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

    final AdminDOMapper adminDOMapper;

    /**
     * 获取所有管理员信息
     */
    @GetMapping("listAdmins")
    public Result<List<AdminDO>> listAdmins() {
        List<AdminDO> adminDOS = adminDOMapper.selectList(null);
        return Result.success(adminDOS);
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


    static private final Lock lock = new ReentrantLock();


    @SneakyThrows
    @PostMapping("/createComp")
    public Result<Void> createComp(@RequestBody CompCreatePO compCreatePO) {

        if (compCreatePO.getStartTimeStamp() == null) {
            compCreatePO.setStartTimeStamp(System.currentTimeMillis() + 30 * 60_000);
        } else {
            if (compCreatePO.getStartTimeStamp() - System.currentTimeMillis() < 30 * 60_1000L) {
                throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("至少设置30分钟以后开始比赛"));
            }
        }

        boolean b = lock.tryLock();
        if (!b) {
            return Result.error(ErrorEnums.CONCURRENCY_VIOLATION.message("不可同时由两个管理员建立比赛"), ExceptionType.BIZ);
        }
        try {
            doCreateComp(compCreatePO);
        } finally {
            lock.unlock();
        }
        return Result.success();
    }
    private void doCreateComp(CompCreatePO compCreatePO) {

        processorManager.clear();

        Ssh.exec("python manage.py empty_data");

        bidDOMapper.selectList(null).forEach(c -> bidDOMapper.deleteById(c.getBidId()));
        compDOMapper.selectList(null).forEach(c -> compDOMapper.deleteById(c.getCompId()));
        clearanceDOMapper.selectList(null).forEach(c -> clearanceDOMapper.deleteById(c.getId()));
        intraInstantDOMapper.selectList(null).forEach(c -> intraInstantDOMapper.deleteById(c.getId()));
        intraQuotationDOMapper.selectList(null).forEach(c -> intraQuotationDOMapper.deleteById(c.getId()));
        unitDOMapper.selectList(null).forEach(c -> unitDOMapper.deleteById(c.getUnitId()));

        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        Integer traderNum = marketSettingDO.getTraderNum();
        Integer robotNum = marketSettingDO.getRobotNum();
        BizEx.trueThrow(traderNum + robotNum > 800, ErrorEnums.PARAM_FORMAT_WRONG.message("人数最多支持120人"));
        List<UserDO> userDOs = userDOMapper.selectList(null).stream().sorted(Comparator.comparing(UserDO::getUserId)).collect(Collectors.toList());
        List<String> traderUserIds = userDOs.subList(0, traderNum).stream().map(UserDO::getUserId).collect(Collectors.toList());
        List<String> robotUserIds = userDOs.subList(traderNum, traderNum + robotNum).stream().map(UserDO::getUserId).collect(Collectors.toList());
        userDOMapper.selectList(null).forEach(userDO -> {
                    userDO.setUserType(null);
                    userDO.setGroupId(null);
                    userDOMapper.updateById(userDO);
                }
        );
        traderUserIds.forEach(traderUserId -> {
            UserDO userDO = userDOMapper.selectById(traderUserId);
            userDO.setUserType(UserType.TRADER);
            userDO.setUserName("交易员" + traderUserId);
            userDOMapper.updateById(userDO);
        });
        robotUserIds.forEach(robotUserId -> {
            UserDO userDO = userDOMapper.selectById(robotUserId);
            userDO.setUserName("机器人" + robotUserId);
            userDO.setUserType(UserType.ROBOT);
            userDOMapper.updateById(userDO);
        });

        Ssh.exec("python manage.py init_data");
        marketSettingDO = marketSettingMapper.selectById(1);
        List<GeneratorBasic> generatorBasics = unitBasicMapper.selectList(null);
        List<IndividualLoadBasic> individualLoadBasics = loadBasicMapper.selectList(null);

        for (MetaUnitDO metaUnitDO : metaUnitDOMapper.selectList(null)) {
            LambdaQueryWrapper<MetaUnitDO> eq = new LambdaQueryWrapper<MetaUnitDO>().eq(MetaUnitDO::getSourceId, metaUnitDO.getSourceId());
            metaUnitDOMapper.delete(eq);
        }

        Integer paperId = marketSettingDO.getPaperId();
        PaperDO paperDO = paperDOMapper.selectById(paperId);
        if (paperDO == null) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("试卷ID:" + paperId + "不存在!"));
        }
        for (GeneratorBasic g : generatorBasics) {
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
                    .renewableType(Kit.enumOf(RenewableType::getDbCode, g.getSubType()).orElse(null))
                    .sourceId(g.getUnitId())
                    .capacity(capacity)
                    .maxCapacity(g.getMaxP())
                    .minCapacity(g.getMinP())
                    .build();
            metaUnitDOMapper.insert(metaUnitDO);
        }

        for (IndividualLoadBasic i : individualLoadBasics) {
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
                    .build();
            metaUnitDOMapper.insert(metaUnitDO);
        }
        delayExecutor.removeStepCommand();
        Map<TradeStage, Integer> bidLengths = StreamMap.<TradeStage, Integer>init()
                .put(TradeStage.AN_INTER, marketSettingDO.getInterprovincialAnnualBidDuration())
                .put(TradeStage.AN_INTRA, marketSettingDO.getIntraprovincialAnnualBidDuration())
                .put(TradeStage.MO_INTER, marketSettingDO.getInterprovincialMonthlyBidDuration())
                .put(TradeStage.MO_INTRA, marketSettingDO.getIntraprovincialMonthlyBidDuration())
                .put(TradeStage.DA_INTRA, marketSettingDO.getIntraprovincialSpotBidDuration())
                .put(TradeStage.DA_INTER, marketSettingDO.getInterprovincialSpotBidDuration())
                .put(TradeStage.ROLL, marketSettingDO.getIntraprovincialSpotRollingBidDuration())
                .put(TradeStage.MULTI_ANNUAL, marketSettingDO.getIntraprovincialMultiYearBidDuration())
                .getMap();

        Map<TradeStage, Integer> resultLengths = StreamMap.<TradeStage, Integer>init()
                .put(TradeStage.AN_INTER, marketSettingDO.getInterprovincialAnnualResultDuration())
                .put(TradeStage.AN_INTRA, marketSettingDO.getIntraprovincialAnnualResultDuration())
                .put(TradeStage.MO_INTER, marketSettingDO.getInterprovincialMonthlyResultDuration())
                .put(TradeStage.MO_INTRA, marketSettingDO.getIntraprovincialMonthlyResultDuration())
                .put(TradeStage.DA_INTRA, marketSettingDO.getIntraprovincialSpotResultDuration())
                .put(TradeStage.DA_INTER, marketSettingDO.getInterprovincialSpotResultDuration())
                .put(TradeStage.ROLL, marketSettingDO.getIntraprovincialSpotRollingResultDuration())
                .put(TradeStage.MULTI_ANNUAL, marketSettingDO.getIntraprovincialMultiYearResultDuration())
                .getMap();

        DelayConfig delayConfig = DelayConfig.builder()
                .marketStageBidLengths(bidLengths)
                .marketStageClearLengths(resultLengths)
                .quitCompeteLength(marketSettingDO.getQuizCompeteDuration())
                .quitResultLength(marketSettingDO.getQuizResultDuration())
                .tradeResultLength(marketSettingDO.getSettleResultDuration())
                .build();


        Integer total = marketSettingDO.getRoundNum();

        gameRankingMapper.selectList(null).forEach(i -> gameRankingMapper.deleteById(i.getTraderId()));
        gameResultMapper.selectList(null).forEach(i -> gameResultMapper.deleteById(i.getId()));

        Stream.of(traderUserIds, robotUserIds).flatMap(Collection::stream).forEach(userId -> {
            GameResult gameResult = new GameResult();
            IntStream.range(1, 1 + total).forEach(roundId -> {
                gameResult.setId(uniqueIdGetter.get());
                gameResult.setRoundId(roundId);
                gameResult.setTraderId(userId);
                gameResultMapper.insert(gameResult);
            });
            GameRanking gameRanking = new GameRanking();
            gameRanking.setTraderId(userId);
            gameRankingMapper.insert(gameRanking);
        });

        // assign metaUnit
        List<Map<String, Collection<MetaUnit>>> roundMetaUnits = IntStream.range(0, marketSettingDO.getRoundNum())
                .mapToObj(roundId -> tunnel.assignMetaUnits(roundId, traderUserIds, robotUserIds)).collect(Collectors.toList());


        Ssh.exec("python manage.py intra_spot_default_bid");

        CompCmd.Create command = CompCmd.Create.builder()
                .compId(uniqueIdGetter.get())
                .userIds(Collect.asList(traderUserIds, robotUserIds).stream().flatMap(Collection::stream).collect(Collectors.toList()))
                .traderIds(traderUserIds)
                .robotIds(robotUserIds)
                .startTimeStamp(compCreatePO.getStartTimeStamp() == null ? Clock.currentTimeMillis() + 30_000L : compCreatePO.getStartTimeStamp())
                .delayConfig(delayConfig)
                .enableQuiz(marketSettingDO.getIsConductingQAndAModule())
                .dt(marketSettingDO.getDt())
                .roundTotal(marketSettingDO.getRoundNum())
                .roundMetaUnits(roundMetaUnits)
                .build();

        CommandBus.accept(command, new HashMap<>());
    }

    /**
     * 手动下一阶段
     */
    @SneakyThrows
    @PostMapping("/step")
    public synchronized Result<Void> step() {
        log.info("Begin step");
        Comp comp = tunnel.runningComp();
        if (comp == null) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("没有运行中的竞赛"));
        } else if (comp.getCompStage() == CompStage.END) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("已经到了最后阶段"));
        }
        delayExecutor.removeStepCommand();

        try {
            bidAspect.stopBidCompletely(30, TimeUnit.SECONDS);
            CompCmd.Step command = CompCmd.Step.builder().stageId(comp.getStageId().next(comp)).build();
            CommandBus.acceptMemoryTransactional(command, new HashMap<>());
        } finally {
            bidAspect.recover();
        }
        log.info("End step");
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
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        ExampleSetting.ExampleSettingBuilder builder = ExampleSetting.builder();

        ForecastError loadMultiYear = ForecastError.builder()
                .forecastError(marketSettingDO.getLoadMaxForecastErr().getMultiYear())
                .transfer(marketSettingDO.getMultiYearLoadForecastDeviation().getTransfer())
                .receiver(marketSettingDO.getMultiYearLoadForecastDeviation().getReceiver())
                .build();
        builder.loadMultiYearMaxForecastErr(loadMultiYear);

            ForecastError loadAnnual = ForecastError.builder()
                    .forecastError(marketSettingDO.getLoadMaxForecastErr().getAnnual())
                    .transfer(marketSettingDO.getAnnualLoadForecastDeviation().getTransfer())
                    .receiver(marketSettingDO.getAnnualLoadForecastDeviation().getReceiver())
                    .build();
            builder.loadAnnualMaxForecastErr(loadAnnual);

            ForecastError loadMonthly = ForecastError.builder()
                    .forecastError(marketSettingDO.getLoadMaxForecastErr().getMonthly())
                    .transfer(marketSettingDO.getMonthlyLoadForecastDeviation().getTransfer())
                    .receiver(marketSettingDO.getMonthlyLoadForecastDeviation().getReceiver())
                    .build();
            builder.loadMonthlyMaxForecastErr(loadMonthly);

            ForecastError loadDa = ForecastError.builder().forecastError(marketSettingDO.getLoadMaxForecastErr().getDa())
                    .transfer(marketSettingDO.getDaLoadForecastDeviation().getTransfer())
                    .receiver(marketSettingDO.getDaLoadForecastDeviation().getReceiver())
                    .build();
            builder.loadDaMaxForecastErr(loadDa);

        ForecastError generatorMultiYear = ForecastError.builder()
                .forecastError(marketSettingDO.getRenewableMaxForecastErr().getMultiYear())
                .transfer(marketSettingDO.getMultiYearRenewableForecastDeviation().getTransfer())
                .receiver(marketSettingDO.getMultiYearRenewableForecastDeviation().getReceiver())
                .build();
        builder.renewableMultiYearMaxForecastErr(generatorMultiYear);


            ForecastError generatorAnnual = ForecastError.builder()
                    .forecastError(marketSettingDO.getRenewableMaxForecastErr().getAnnual())
                    .transfer(marketSettingDO.getAnnualRenewableForecastDeviation().getTransfer())
                    .receiver(marketSettingDO.getAnnualRenewableForecastDeviation().getReceiver())
                    .build();
            builder.renewableAnnualMaxForecastErr(generatorAnnual);

            ForecastError generatorMonthly = ForecastError.builder()
                    .forecastError(marketSettingDO.getRenewableMaxForecastErr().getMonthly())
                    .transfer(marketSettingDO.getMonthlyRenewableForecastDeviation().getTransfer())
                    .receiver(marketSettingDO.getMonthlyRenewableForecastDeviation().getReceiver())
                    .build();
            builder.renewableMonthlyMaxForecastErr(generatorMonthly);

            ForecastError generatorDa = ForecastError.builder()
                    .forecastError(marketSettingDO.getRenewableMaxForecastErr().getDa())
                    .transfer(marketSettingDO.getDaRenewableForecastDeviation().getTransfer())
                    .receiver(marketSettingDO.getDaRenewableForecastDeviation().getReceiver())
                    .build();
            builder.renewableDaMaxForecastErr(generatorDa);

        ExampleSetting exampleSetting = builder.build();
        return Result.success(exampleSetting);
    }


    /**
     * 更新算例参数
     */
    @PostMapping("updateExampleSetting")
    public Result<Void> updateExampleSetting(@RequestBody ExampleSetting exampleSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);

        ForecastError loadMultiYearMaxForecastErr = exampleSetting.getLoadMultiYearMaxForecastErr();
        ForecastError loadAnnualMaxForecastErr = exampleSetting.getLoadAnnualMaxForecastErr();
        ForecastError loadMonthlyMaxForecastErr = exampleSetting.getLoadMonthlyMaxForecastErr();
        ForecastError loadDaMaxForecastErr = exampleSetting.getLoadDaMaxForecastErr();

        ForecastErr loadForecastErr = ForecastErr.builder()
                .multiYear(loadMultiYearMaxForecastErr.getForecastError())
                .annual(loadAnnualMaxForecastErr.getForecastError())
                .monthly(loadMonthlyMaxForecastErr.getForecastError())
                .da(loadDaMaxForecastErr.getForecastError())
                .build();
        marketSettingDO.setLoadMaxForecastErr(loadForecastErr);

        marketSettingDO.setMultiYearLoadForecastDeviation(RealErr.builder().transfer(loadMultiYearMaxForecastErr.getTransfer()).receiver(loadMultiYearMaxForecastErr.getReceiver()).build());
        marketSettingDO.setAnnualLoadForecastDeviation(RealErr.builder().transfer(loadAnnualMaxForecastErr.getTransfer()).receiver(loadAnnualMaxForecastErr.getReceiver()).build());
        marketSettingDO.setMonthlyLoadForecastDeviation(RealErr.builder().transfer(loadMonthlyMaxForecastErr.getTransfer()).receiver(loadMonthlyMaxForecastErr.getReceiver()).build());
        marketSettingDO.setDaLoadForecastDeviation(RealErr.builder().transfer(loadDaMaxForecastErr.getTransfer()).receiver(loadDaMaxForecastErr.getReceiver()).build());


        ForecastError renewableMultiYearMaxForecastErr = exampleSetting.getRenewableMultiYearMaxForecastErr();
        ForecastError renewableAnnualMaxForecastErr = exampleSetting.getRenewableAnnualMaxForecastErr();
        ForecastError renewableMonthlyMaxForecastErr = exampleSetting.getRenewableMonthlyMaxForecastErr();
        ForecastError renewableDaMaxForecastErr = exampleSetting.getRenewableDaMaxForecastErr();


        ForecastErr renewableForecastErr = ForecastErr.builder().multiYear(renewableMultiYearMaxForecastErr.getForecastError())
                .annual(renewableAnnualMaxForecastErr.getForecastError())
                .monthly(renewableMonthlyMaxForecastErr.getForecastError())
                .da(renewableDaMaxForecastErr.getForecastError())
                .build();
        marketSettingDO.setRenewableMaxForecastErr(renewableForecastErr);

        marketSettingDO.setMultiYearRenewableForecastDeviation(RealErr.builder().transfer(renewableMultiYearMaxForecastErr.getTransfer()).receiver(renewableMultiYearMaxForecastErr.getReceiver()).build());
        marketSettingDO.setAnnualRenewableForecastDeviation(RealErr.builder().transfer(renewableAnnualMaxForecastErr.getTransfer()).receiver(renewableAnnualMaxForecastErr.getReceiver()).build());
        marketSettingDO.setMonthlyRenewableForecastDeviation(RealErr.builder().transfer(renewableMonthlyMaxForecastErr.getTransfer()).receiver(renewableMonthlyMaxForecastErr.getReceiver()).build());
        marketSettingDO.setDaRenewableForecastDeviation(RealErr.builder().transfer(renewableDaMaxForecastErr.getTransfer()).receiver(renewableDaMaxForecastErr.getReceiver()).build());


        marketSettingMapper.updateById(marketSettingDO);
        return Result.success();
    }



    /**
     * 查看知识竞答参数
     */
    @GetMapping("getQuizSetting")
    public Result<QuizSetting> getQuizSetting() {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
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
    @PostMapping("updateQuizSetting")
    public Result<Void> updateQuizSetting(@RequestBody QuizSetting quizSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
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
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        ElectricMarketSettingVO electricMarketSettingVO = ElectricMarketSettingVO.builder()
                .generatorPriceLimit(GridLimit.builder().high(marketSettingDO.getOfferPriceCap()).low(marketSettingDO.getOfferPriceFloor()).build())
                .loadPriceLimit(GridLimit.builder().high(marketSettingDO.getBidPriceCap()).low(marketSettingDO.getBidPriceFloor()).build())
                .capacityPrice(marketSettingDO.getCapacityPrice())
                .maxForwardUnitPositionInterest(marketSettingDO.getMaxForwardUnitPositionInterest())
                .maxForwardLoadPositionInterest(marketSettingDO.getMaxForwardLoadPositionInterest())
                .regulatedProducerPrice(marketSettingDO.getRegulatedProducerPrice())
                .regulatedInterprovTransmissionPrice(marketSettingDO.getRegulatedInterprovTransmissionPrice())
                .regulatedUserTariff(marketSettingDO.getRegulatedUserTariff())
                .forwardNumBidSegs(marketSettingDO.getForwardNumBidSegs())
                .forwardNumOfferSegs(marketSettingDO.getForwardNumBidSegs())
                .spotNumBidSegs(marketSettingDO.getSpotNumBidSegs())
                .spotNumOfferSegs(marketSettingDO.getSpotNumOfferSegs())
                .interprovClearingMode(tradingMode.get(marketSettingDO.getInterprovClearingMode()))
                .interprovTradingMode(marketSettingDO.getInterprovTradingMode())
                .retailPriceForecastMultiple(marketSettingDO.getRetailPriceForecastMultiple())
                .singleLoginLimit(marketSettingDO.getSingleLoginLimit())
                .minForwardLoadPosition(marketSettingDO.getMinForwardLoadPosition())
                .minForwardUnitPosition(marketSettingDO.getMinForwardUnitPosition())
                .maxForwardClearedMwMultiple(marketSettingDO.getMaxForwardClearedMwMultiple())
                .solarSpecificPriceCap(marketSettingDO.getSolarSpecificPriceCap())
                .windSpecificPriceCap(marketSettingDO.getWindSpecificPriceCap())
                .renewableSpecialTransactionDemandPercentage(RenewableSpecialTransactionDemandPercentage.resolve(marketSettingDO.getRenewableSpecialTransactionDemandPercentage()))
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
    @PostMapping("updateElectricMarketSetting")
    public Result<Void> updateElectricMarketSetting(@RequestBody ElectricMarketSetting electricMarketSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        marketSettingDO.setOfferPriceCap(electricMarketSetting.getGeneratorPriceLimit().getHigh());
        marketSettingDO.setOfferPriceFloor(electricMarketSetting.getGeneratorPriceLimit().getLow());
        marketSettingDO.setBidPriceCap(electricMarketSetting.getLoadPriceLimit().getHigh());
        marketSettingDO.setBidPriceFloor(electricMarketSetting.getLoadPriceLimit().getLow());
        marketSettingDO.setMaxForwardLoadPositionInterest(electricMarketSetting.getMaxForwardLoadPositionInterest());
        marketSettingDO.setMaxForwardUnitPositionInterest(electricMarketSetting.getMaxForwardUnitPositionInterest());
        marketSettingDO.setRegulatedProducerPrice(electricMarketSetting.getRegulatedProducerPrice());
        marketSettingDO.setRegulatedUserTariff(electricMarketSetting.getRegulatedUserTariff());
        marketSettingDO.setRegulatedInterprovTransmissionPrice(electricMarketSetting.getRegulatedInterprovTransmissionPrice());
        marketSettingDO.setRetailPriceForecastMultiple(electricMarketSetting.getRetailPriceForecastMultiple());
        marketSettingDO.setSingleLoginLimit(electricMarketSetting.getSingleLoginLimit());
        marketSettingDO.setMinForwardLoadPosition(electricMarketSetting.getMinForwardLoadPosition());
        marketSettingDO.setMinForwardLoadPosition(electricMarketSetting.getMinForwardLoadPosition());
        marketSettingDO.setMaxForwardClearedMwMultiple(electricMarketSetting.getMaxForwardClearedMwMultiple());
        marketSettingDO.setWindSpecificPriceCap(electricMarketSetting.getWindSpecificPriceCap());
        marketSettingDO.setSolarSpecificPriceCap(electricMarketSetting.getSolarSpecificPriceCap());

        marketSettingDO.setRenewableSpecialTransactionDemandPercentage(electricMarketSetting.getRenewableSpecialTransactionDemandPercentage().storeValue());

        marketSettingMapper.updateById(marketSettingDO);
        return Result.success();
    }


    /**
     * 获取竞赛仿真参数
     */
    @GetMapping("getSimulateSetting")
    public Result<SimulateSetting> getSimulateSetting() {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        SimulateSetting simulateSetting = Convertor.INST.to(marketSettingDO);
        String assetAllocationModeStr = marketSettingDO.getAssetAllocationModeStr();
        List<String> assetAllocationModes = Arrays.stream(StringUtils.split(assetAllocationModeStr, ":")).collect(Collectors.toList());
        simulateSetting.setAssetAllocationModes(assetAllocationModes);
        simulateSetting.setAssetAllocationMode(marketSettingDO.getAssetAllocationMode());

        Integer roundNum = marketSettingDO.getRoundNum();
        String[] caseSettings = marketSettingDO.getCaseSetting().split(":");
        BizEx.trueThrow(caseSettings.length != roundNum, ErrorEnums.PARAM_FORMAT_WRONG.message("caseSetting参数异常，不与轮次匹配"));
        String[] transmissionAndDistributionTariffs = marketSettingDO.getTransmissionAndDistributionTariff().split(":");
        BizEx.trueThrow(transmissionAndDistributionTariffs.length != roundNum, ErrorEnums.PARAM_FORMAT_WRONG.message("transmissionAndDistributionTariff参数异常，不与轮次匹配"));
        String[] multiYearCoalPrices = marketSettingDO.getMultiYearCoalPrice().split(":");
        BizEx.trueThrow(multiYearCoalPrices.length/2 != roundNum, ErrorEnums.PARAM_FORMAT_WRONG.message("multiYearCoalPrices参数异常，不与轮次匹配"));
        String[] annualCoalPrices = marketSettingDO.getAnnualCoalPrice().split(":");
        BizEx.trueThrow(annualCoalPrices.length/2 != roundNum, ErrorEnums.PARAM_FORMAT_WRONG.message("annualCoalPrice参数异常，不与轮次匹配"));
        String[] monthlyCoalPrices = marketSettingDO.getMonthlyCoalPrice().split(":");
        BizEx.trueThrow(monthlyCoalPrices.length/2 != roundNum, ErrorEnums.PARAM_FORMAT_WRONG.message("monthlyCoalPrice参数异常，不与轮次匹配"));
        String[] daCoalPrices = marketSettingDO.getDaCoalPrice().split(":");
        BizEx.trueThrow(daCoalPrices.length/2 != roundNum, ErrorEnums.PARAM_FORMAT_WRONG.message("daCoalPrice参数异常，不与轮次匹配"));
        List<RoundSetting> roundSettings = IntStream.range(0, roundNum).mapToObj(i -> {
            RoundSetting roundSetting = new RoundSetting();
            char[] charArray = caseSettings[i].toCharArray();
            roundSetting.setTransferDiffer(Objects.equals(charArray[0], '1'));
            roundSetting.setTransferLoadPeak(Objects.equals(charArray[1], '1'));
            roundSetting.setReceiverDiffer(Objects.equals(charArray[2], '1'));
            roundSetting.setReceiverLoadPeak(Objects.equals(charArray[3], '1'));
            roundSetting.setTransmissionAndDistributionTariff(Double.parseDouble(transmissionAndDistributionTariffs[i]));
            roundSetting.setTransferMultiCoalPrice(Double.parseDouble(multiYearCoalPrices[i]));
            roundSetting.setReceiverMultiCoalPrice(Double.parseDouble(multiYearCoalPrices[i + roundNum]));
            roundSetting.setTransferAnnualCoalPrice(Double.parseDouble(annualCoalPrices[i]));
            roundSetting.setReceiverAnnualCoalPrice(Double.parseDouble(annualCoalPrices[i + roundNum]));
            roundSetting.setTransferMonthlyCoalPrice(Double.parseDouble(monthlyCoalPrices[i]));
            roundSetting.setReceiverMonthlyCoalPrice(Double.parseDouble(monthlyCoalPrices[i + roundNum]));
            roundSetting.setTransferDaCoalPrice(Double.parseDouble(daCoalPrices[i]));
            roundSetting.setReceiverDaCoalPrice(Double.parseDouble(daCoalPrices[i + roundNum]));
            return roundSetting;
        }).collect(Collectors.toList());
        simulateSetting.setRoundSettings(roundSettings);

        simulateSetting.setIntraprovincialMultiYearBidDuration(simulateSetting.getIntraprovincialMultiYearBidDuration());
        simulateSetting.setIntraprovincialMultiYearResultDuration(simulateSetting.getIntraprovincialMultiYearResultDuration());


        simulateSetting.setMwhPercentageForRetailPlan(marketSettingDO.getMwhPercentageForRetailPlan());

        return Result.success(simulateSetting);
    }


    /**
     * 更新竞赛仿真参数
     */
    @PostMapping("updateSimulateSetting")
    public Result<Void> updateSimulateSetting(@RequestBody SimulateSetting simulateSetting) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
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
        marketSettingDO.setTraderOfferMode(simulateSetting.getTraderOfferMode());
        marketSettingDO.setRobotOfferMode(simulateSetting.getRobotOfferMode());
        marketSettingDO.setIsEnteringReviewStage(simulateSetting.getIsEnteringReviewStage());
        marketSettingDO.setRoundNum(simulateSetting.getRoundNum());
        marketSettingDO.setAssetAllocationMode(simulateSetting.getAssetAllocationMode());
        marketSettingDO.setIsOpeningIntraprovSpotQuickOffer(simulateSetting.getIsOpeningIntraprovSpotQuickOffer());
        marketSettingDO.setIsOpeningThermalStartOffer(simulateSetting.getIsOpeningThermalStartOffer());
        marketSettingDO.setIsOpeningThermalMinoutputOffer(simulateSetting.getIsOpeningThermalMinoutputOffer());
        marketSettingDO.setIntraprovincialSpotRollingBidDuration(simulateSetting.getIntraprovincialSpotRollingBidDuration());
        marketSettingDO.setIntraprovincialSpotRollingResultDuration(simulateSetting.getIntraprovincialSpotRollingResultDuration());

        marketSettingDO.setIntraprovincialMultiYearBidDuration(simulateSetting.getIntraprovincialMultiYearBidDuration());
        marketSettingDO.setIntraprovincialMultiYearResultDuration(simulateSetting.getIntraprovincialMultiYearResultDuration());

        // 轮次校验
        Integer roundNum = marketSettingDO.getRoundNum();
        if (roundNum != simulateSetting.getRoundSettings().size()) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("轮次数量不匹配"));
        }

        String caseSetting = simulateSetting.getRoundSettings().stream().map(r -> {
            return (r.getTransferDiffer() ? "1" : "0") +
                    (r.getTransferLoadPeak() ? "1" : "0") +
                    (r.getReceiverDiffer() ? "1" : "0") +
                    (r.getReceiverLoadPeak() ? "1" : "0");
        }).collect(Collectors.joining(":"));

        marketSettingDO.setCaseSetting(caseSetting);

        String transmissionAndDistributionTariff = simulateSetting.getRoundSettings().stream().map(r -> r.getTransmissionAndDistributionTariff() + "").collect(Collectors.joining(":"));
        marketSettingDO.setTransmissionAndDistributionTariff(transmissionAndDistributionTariff);

        String transferMultiAnnualCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getTransferMultiCoalPrice() + "").collect(Collectors.joining(":"));
        String receiverMultiAnnualCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getReceiverMultiCoalPrice() + "").collect(Collectors.joining(":"));
        marketSettingDO.setMultiYearCoalPrice(transferMultiAnnualCoalPrice + ":" + receiverMultiAnnualCoalPrice);

        String transferAnnualCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getTransferAnnualCoalPrice() + "").collect(Collectors.joining(":"));
        String receiverAnnualCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getReceiverAnnualCoalPrice() + "").collect(Collectors.joining(":"));
        marketSettingDO.setAnnualCoalPrice(transferAnnualCoalPrice + ":" + receiverAnnualCoalPrice);

        String transferMonthlyCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getTransferMonthlyCoalPrice() + "").collect(Collectors.joining(":"));
        String receiverMonthlyCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getReceiverMonthlyCoalPrice() + "").collect(Collectors.joining(":"));
        marketSettingDO.setMonthlyCoalPrice(transferMonthlyCoalPrice + ":" + receiverMonthlyCoalPrice);

        String transferDaCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getTransferDaCoalPrice() + "").collect(Collectors.joining(":"));
        String receiverDaCoalPrice = simulateSetting.getRoundSettings().stream().map(r -> r.getReceiverDaCoalPrice() + "").collect(Collectors.joining(":"));
        marketSettingDO.setDaCoalPrice(transferDaCoalPrice + ":" + receiverDaCoalPrice);

        marketSettingDO.setMwhPercentageForRetailPlan(simulateSetting.getMwhPercentageForRetailPlan());

        marketSettingMapper.updateById(marketSettingDO);
        return Result.success();
    }

    final RetailPlanParameterDOMapper retailPlanParameterDOMapper;


    /**
     * 列举所有的套餐详情
     */
    @GetMapping("listRetailPlans")
    public Result<List<RetailPlan>> listRetailPlans() {
        ListMultimap<Integer, RetailPlanParameter> retailPlans=
                retailPlanParameterDOMapper.selectList(null)
                .stream().collect(Collect.listMultiMap(RetailPlanParameter::getRetailPlanId));
        List<RetailPlan> retailPlanVOs = retailPlans.asMap().entrySet().stream().map(e -> {
            List<Parameter> parameters = e.getValue().stream().map(p -> {
                return Parameter.builder().parameterId(p.getParameterId())
                        .parameterDescription(p.getParameterDescription())
                        .value(p.getParameterValue())
                        .build();
            }).collect(Collectors.toList());
            return RetailPlan.builder().retailPlanId(e.getKey()).parameters(parameters).build();
        }).collect(Collectors.toList());
        return Result.success(retailPlanVOs);
    }

    /**
     * 提交参数参数
     * @param retailPlans 参套参数
     */
    @PostMapping("submitRetailPlans")
    public Result<Void> submitRetailPlans(@RequestBody @Valids List<RetailPlan> retailPlans) {
        retailPlans.forEach(retailPlan -> {
            for (Parameter parameter : retailPlan.parameters) {
                LambdaUpdateWrapper<RetailPlanParameter> eq = new LambdaUpdateWrapper<RetailPlanParameter>()
                        .eq(RetailPlanParameter::getRetailPlanId, retailPlan.retailPlanId)
                        .eq(RetailPlanParameter::getParameterId, parameter.getParameterId());
                RetailPlanParameter retailPlanParameter = RetailPlanParameter.builder().parameterValue(parameter.value).build();
                retailPlanParameterDOMapper.update(retailPlanParameter, eq);
            }
        });
        return Result.success();
    }




    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class RetailPlan {
        /**
         * 套餐id
         */
        @NotNull(message = "套餐id不能为空")
        Integer retailPlanId;

        /**
         * 套餐参数
         */
        @NotEmpty(message = "套餐参数不应该为空")
        List<Parameter> parameters;
    }

    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Parameter {

        /**
         * 参数id
         */
        @NotNull(message = "参数id不能为空")
        Integer parameterId;

        /**
         * 参数描述
         */
        String parameterDescription;

        /**
         * 参数值
         */
        @NotBlank(message = "参数值不能为空")
        String value;

    }






    final PaperDOMapper paperDOMapper;
    /**
     * 删除试卷
     * @param paperId 试卷Id
     */
    @PostMapping("deletePaper")
    public Result<Void> deletePaper(@NotNull Long paperId) {
        paperDOMapper.deleteById(paperId);
        return Result.success();
    }

    /**
     * 试卷列表
     */
    @GetMapping("listPapers")
    public Result<List<PaperVO>> listPapers() {
        List<PaperDO> paperDOs = paperDOMapper.selectList(null);
        List<PaperVO> paperVOs = paperDOs.stream().map(p -> new PaperVO(p.getId(), p.getName(), p.getQuestions())).collect(Collectors.toList());
        return Result.success(paperVOs);
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
