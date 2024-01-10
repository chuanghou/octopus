package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "game_ranking",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameRanking {

  @TableId(type = IdType.INPUT)
  String traderId;
  Integer totalRanking = 0;

}
