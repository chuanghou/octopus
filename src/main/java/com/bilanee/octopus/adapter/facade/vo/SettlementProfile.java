package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettlementProfile {

    /**
     * 送电机组总收入
     */
    Double transferGeneratorTotalIncome;

    /**
     * 送电省负荷总支出
     */
    Double transferLoadTotalConsume;

    /**
     * 受电机组总收入
     */
    Double receiverGeneratorTotalIncome;

    /**
     * 受电省负荷总支出
     */
    Double receiverLoadTotalConsume;

    /**
     * 省间交易总金额
     */
    Double interTradeTotalMoney;

    /**
     * 跨省输电成本
     */
    Double interTransmissionCost;

    /**
     * 送电省收入细节
     */
    ProvinceProfile transfer;

    /**
     * 受电省收入细节
     */
    ProvinceProfile receiver;


}
