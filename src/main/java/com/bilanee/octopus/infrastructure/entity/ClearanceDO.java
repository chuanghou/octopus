package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_clearance_do",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClearanceDO {

    @TableId(type = IdType.AUTO)
    Long id;
    String stageId;
    TimeFrame timeFrame;
    Province province;
    String clearance;

}
