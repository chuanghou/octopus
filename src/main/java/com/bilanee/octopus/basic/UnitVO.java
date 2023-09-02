package com.bilanee.octopus.basic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitVO {

    /**
     * 单元id
     */
    Long unitId;

    /**
     * 单元名称
     */
    String name;

    /**
     * 买方向剩余持仓，NULL代表此时不能发生买动作
     */
    Double buyBalance;

    /**
     * 卖方向剩余持仓，NULL代表此时不能发生卖动作
     */
    Double sellBalance;


}
