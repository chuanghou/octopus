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
@TableName(value = "renewable_unit_forecast",autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratorForecastValueDO {

  @TableId(type = IdType.INPUT)
  Long id;
  String dt;
  Integer prd;
  Double annualPForecast;
  Double monthlyPForecast;
  Double daPForecast;
  Double rtP;
  Integer unitId;

}
