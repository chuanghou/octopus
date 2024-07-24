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
@TableName(value = "intraprovincial_forward_market_transaction_status", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForwardMarketTransactionStatusDO {

    @TableId(type = IdType.INPUT)
    Long id;
    Integer roundId;
    Integer prov;
    Integer pfvPrd;
    Integer marketType;
    Double avgClearedPrice;

}