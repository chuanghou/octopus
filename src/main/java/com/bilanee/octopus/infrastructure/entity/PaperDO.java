package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.infrastructure.handlers.ListQuestionHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_paper", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaperDO {

    @TableId(type = IdType.AUTO)
    Integer id;

    String name;

    @TableField(typeHandler = ListQuestionHandler.class)
    List<Question> questions;

}
