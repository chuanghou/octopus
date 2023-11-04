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
public class GameRanking {

  String traderId;
  Integer totalRanking;

}
