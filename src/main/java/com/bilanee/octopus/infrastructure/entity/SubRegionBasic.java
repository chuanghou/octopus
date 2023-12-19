package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "subregion_basic", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubRegionBasic {

  @TableId(type = IdType.INPUT)
  Integer subregionId;
  String subregionName;
  Integer prov;

}
