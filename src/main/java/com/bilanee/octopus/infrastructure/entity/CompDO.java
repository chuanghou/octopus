package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.domain.DelayConfig;
import com.bilanee.octopus.infrastructure.handlers.ListStepRecordHandler;
import com.stellariver.milky.domain.support.base.BaseDataObject;
import com.stellariver.milky.infrastructure.base.database.AbstractMpDO;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_comp_do", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompDO extends AbstractMpDO implements BaseDataObject<Long> {

    @TableId(type = IdType.INPUT)
    Long compId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    List<String> userIds;
    CompStage compStage;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    Integer roundId;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    TradeStage tradeStage;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    MarketStatus marketStatus;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    Long endingTimeStamp;
    @TableField(typeHandler = JacksonTypeHandler.class)
    DelayConfig delayConfig;

    String dt;

    Boolean enableQuiz;

    Integer roundTotal;

    @TableField(typeHandler = ListStepRecordHandler.class)
    List<StepRecord> stepRecords = new ArrayList<>();

    @Override
    public Long getPrimaryId() {
        return compId;
    }
}
