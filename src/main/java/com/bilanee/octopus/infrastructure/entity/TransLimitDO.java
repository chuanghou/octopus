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
@TableName("forward_receiving_province_unmet_demand")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransLimitDO {

    @TableId(type = IdType.INPUT)
    Long id;
    Integer pfvPrd;
    Double minAnnualReceivingMw;
    Double minMonthlyReceivingMw;
    Double maxAnnualReceivingMw;
    Double maxMonthlyReceivingMw;
    Integer roundId;

}
