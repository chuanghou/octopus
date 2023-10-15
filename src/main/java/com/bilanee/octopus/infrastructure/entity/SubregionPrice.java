package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubregionPrice {

  @TableId
  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Double daLmp;
  Double rtLmp;
  Integer subregionId;

}
