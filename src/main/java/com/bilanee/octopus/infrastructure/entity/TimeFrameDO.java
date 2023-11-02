package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "pfv_period_definition_of_forward_market", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TimeFrameDO {

  Integer prd;
  Integer sendingPfvPrd;
  Integer receivingPfvPrd;

}
