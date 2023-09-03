package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bilanee.octopus.common.enums.CompStage;
import com.bilanee.octopus.common.enums.MarketStatus;
import com.bilanee.octopus.common.enums.TradeStage;
import com.stellariver.milky.domain.support.base.BaseDataObject;
import com.stellariver.milky.infrastructure.base.database.AbstractMpDO;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("octopus_comp_do")
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompDO extends AbstractMpDO implements BaseDataObject<Long> {

    @TableId(type = IdType.INPUT)
    Long compId;
    CompStage compStage;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    Integer roundId;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    TradeStage tradeStage;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    MarketStatus marketStatus;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    Long endingTimeStamp;

    @Override
    public Long getPrimaryId() {
        return compId;
    }
}
