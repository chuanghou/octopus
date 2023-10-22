package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.enums.Province;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProvinceProfile {
    /**
     * 省份标识
     */
     Province province;

    /**
     * 电网企业收入（元）
     */
    Double gridIncome;

    /**
     * 省内双轨制不平衡资金（元）
     */
    Double intraNotBalance;

    /**
     * 省间双轨制不平衡资金（元）
     */
    Double interNotBalance;

    /**
     * 机组运行补偿费用（元）
     */
    Double generatorCompetent;

    /**
     * 偏差收益回收资金（元）
     */
    Double balanceMargin;

}
