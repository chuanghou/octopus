package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "retail_plan_description", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RetailMarketPackageDescription {

    Integer roundId;
    Integer prov;
    Integer retailPlanId;
    String retailPlanDescription;

}
