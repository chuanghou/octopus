package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "retail_plan_parameter", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RetailPlanParameter {

    /**
     * 套餐id
     */
    Integer retailPlanId;

    /**
     * 参数id 
     */
    Integer parameterId;

    /**
     * 参数描述
     */
    String parameterDescription;

    /**
     * 参数值
     */
    String parameterValue;

}
