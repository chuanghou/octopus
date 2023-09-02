package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.BasicConvertor;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.CompCommand;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;


@RestController
@RequiredArgsConstructor
@RequestMapping("manage")
public class ManageFacade {

    final UniqueIdGetter uniqueIdGetter;
    final DomainTunnel domainTunnel;
    final CompDOMapper compDOMapper;

    /**
     * 竞赛创建接口
     * @param compCreatePO 创建竞赛参数
     * @return 创建结果
     */
    @PostMapping("/createComp")
    public Result<Void> createComp(@RequestBody CompCreatePO compCreatePO) {
        CompCommand.Create command = Convertor.INST.to(compCreatePO);
        command.setCompId(uniqueIdGetter.get());
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
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
