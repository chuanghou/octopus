package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.facade.po.CompCreatePO;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.facade.vo.UserVO;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.CompCmd;
import com.bilanee.octopus.infrastructure.entity.UserDO;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UserDOMapper;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapping;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

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

    /**
     * 竞赛新建接口
     * @param compCreatePO 创建竞赛参数
     */
    @PostMapping("/createComp")
    public Result<Void> createComp(@RequestBody CompCreatePO compCreatePO) {
        delayExecutor.removeStepCommand();
        CompCmd.Create command = Convertor.INST.to(compCreatePO);
        if (command.getStartTimeStamp() == null) {
            command.setStartTimeStamp(Clock.currentTimeMillis() + 5 * 1000L);
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

        @AfterMapping
        default void afterMapping(CompCreatePO compCreatePO, @MappingTarget CompCmd.Create create) {
            String dt = Kit.isBlank(compCreatePO.getDt()) ? Clock.todayString() : compCreatePO.getDt();
            create.setDt(dt);
        }

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompVO to(Comp comp);


        @BeanMapping(builder = @Builder(disableBuilder = true))
        default String toString(StageId stageId) {
            return stageId.toString();
        }

    }

}
