package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("intraprovincial_spot_load_cleared")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotLoadCleared {

  Long id;
  Integer roundId;
  String dt;
  Integer prd;
  Double daClearedMw;
  Integer loadId;

}
