package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "game_result",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameResult {

  Long id;
  Integer roundId;
  String dt;
  String traderId;
  Double profit;
  Integer ranking;
  Double averageProfitOfSameType;

}
