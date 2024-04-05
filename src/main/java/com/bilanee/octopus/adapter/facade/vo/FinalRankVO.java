package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FinalRankVO {

    /**
     * 我的最终排名
     */
    Ranking myFinalRanking;

    /**
     * 我的各轮排名
     */
    List<Ranking> roundRankings;

    /**
     * 最终总排名
     */
    List<Ranking> finalRankings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Ranking {

        /**
         * 组别
         */
        String groupId;

        /**
         *
         */
        Integer number;

        /**
         * 姓名
         */
        String userId;

        /**
         * 分数
         */
        Double score;

        /**
         * 绝对利润
         */
        Double profit;
    }
}
