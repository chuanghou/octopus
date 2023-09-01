package com.bilanee.octopus.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.basic.TradeStage;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.CompCommand;
import com.bilanee.octopus.infrastructure.entity.CompDO;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/comp")
public class CompFacade {

    final UniqueIdGetter uniqueIdGetter;
    final DomainTunnel domainTunnel;
    final CompDOMapper compDOMapper;

    @PostMapping("/createComp")
    public Result<Void> createComp(@RequestBody CompCreatePO compCreatePO) {
        CompCommand.Create command = Convertor.INST.to(compCreatePO);
        command.setCompId(uniqueIdGetter.get());
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
    }


    @GetMapping("runningComp")
    public Result<CompVO> runningComp() {
        LambdaQueryWrapper<CompDO> queryWrapper = new LambdaQueryWrapper<CompDO>().orderByDesc(CompDO::getCompId).last("LIMIT 1");
        CompDO compDO = compDOMapper.selectOne(queryWrapper);
        Comp comp = domainTunnel.getByAggregateId(Comp.class, compDO.getCompId());
        return Result.success(Convertor.INST.to(comp));
    }



    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompCommand.Create to(CompCreatePO compCreatePO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompVO to(Comp comp);

    }





}
