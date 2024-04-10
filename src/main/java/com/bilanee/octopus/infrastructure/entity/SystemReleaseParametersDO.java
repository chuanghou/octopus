package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.enums.Choice;
import com.bilanee.octopus.infrastructure.handlers.ListChoiceHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "system_parameter_release", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemReleaseParametersDO {

    Integer prov;
    Integer prd;
    Double renewableGovernmentSubsidy;

}