package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ranking {

    /**
     * 名次，从1开始
     */
    Integer order;

    /**
     * 用户名
     */
    String userName;

    /**
     * 归一化利润百分比
     */
    String percent;
}
