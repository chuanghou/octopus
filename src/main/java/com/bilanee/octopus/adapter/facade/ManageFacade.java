package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.facade.po.CompCreatePO;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.facade.vo.UserVO;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.CompCmd;
import com.bilanee.octopus.infrastructure.entity.UserDO;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UserDOMapper;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.*;
import org.mapstruct.Mapping;
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
     * 竞赛创建接口
     * @param compCreatePO 创建竞赛参数
     */
    @PostMapping("/createComp")
    public Result<Void> createComp(@RequestBody CompCreatePO compCreatePO) {
        CompCmd.Create command = Convertor.INST.to(compCreatePO);
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
        @Mapping(source = "compInitLength", target = "delayConfig.compInitLength")
        @Mapping(source = "quitCompeteLength", target = "delayConfig.quitCompeteLength")
        @Mapping(source = "quitResultLength", target = "delayConfig.quitResultLength")
        @Mapping(source = "marketStageBidLengths", target = "delayConfig.marketStageBidLengths")
        @Mapping(source = "marketStageClearLengths", target = "delayConfig.marketStageClearLengths")
        @Mapping(source = "tradeResultLength", target = "delayConfig.tradeResultLength")
        CompCmd.Create to(CompCreatePO compCreatePO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompVO to(Comp comp);

    }

}
