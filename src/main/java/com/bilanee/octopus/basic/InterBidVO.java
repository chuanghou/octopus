package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterBidVO {

    /**
     * 运行时段
     */
    TimeFrame timeFrame;

    /**
     * 系统容量
     */
    Double capacity;

    /**
     * 买卖两个方向持仓限制，当持仓是0的时候，表明此时不能下该方向报报单
     */
    List<BalanceVO> balanceVOs;

    /**
     * 后台记录的上一次报单，当没有的报单的情况，这个是个空的数组
     */
    List<BidVO> bidVOs;

}
