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
@TableName(value = "system_parameter_release", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemReleaseParametersDO {

    @TableId(type = IdType.INPUT)
    Long id;
    Integer prov;
    Integer prd;
    Double renewableGovernmentSubsidy;
    Integer roundId;

}