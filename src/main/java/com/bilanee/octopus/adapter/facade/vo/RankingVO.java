package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RankingVO {

    /**
     * 表头文字
     */
    String headLine;

    /**
     * 用户自身排名
     */
    Ranking myRanking;

    /**
     * 所有用户总排名细节
     */
    List<Ranking> rankings;



}
