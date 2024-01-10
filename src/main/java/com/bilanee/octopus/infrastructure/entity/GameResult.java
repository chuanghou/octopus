package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.stellariver.milky.common.tool.common.Clock;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "game_result",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameResult {

  @TableId(type = IdType.AUTO)
  Long id;
  Integer roundId;
  String dt = Clock.todayString();
  String traderId;
  Double profit = 0D;
  Integer ranking = 0;
  Double averageProfitOfSameType = 0D;

}
