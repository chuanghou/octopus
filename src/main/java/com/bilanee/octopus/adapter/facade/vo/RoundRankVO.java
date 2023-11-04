package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoundRankVO {

    /**
     * 标题
     */
    String headLine;

    /**
     * 我的本轮排名
     */
    Ranking myRanking;

    /**
     * 本轮所有排名列表
     */
    List<Ranking> rankings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Ranking {

        /**
         * 排名id
         */
        Integer ranking;

        /**
         * 交易员id
         */
        String userId;

        /**
         * 利润
         */
        Double profit;
    }
}
