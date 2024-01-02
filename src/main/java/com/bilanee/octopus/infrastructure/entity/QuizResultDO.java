package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.TokenUtils;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("octopus_quiz_result")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizResultDO {

    @TableId(type = IdType.INPUT)
    String userId;
    Integer score;

}
