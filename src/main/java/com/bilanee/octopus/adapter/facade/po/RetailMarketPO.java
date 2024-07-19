package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Valids;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RetailMarketPO {

    /**
     * 单元ID
     */
    @NotNull @Positive
    Long unitId;

    /**
     * 套餐选择结果
     */
    @NotNull(message = "套餐选项不能为空") @Valids
    List<PackageChoice> packageChoices;


    @AfterValidation
    public void afterValidation() {
        packageChoices.forEach(p -> BizEx.nullThrow(p, ErrorEnums.PARAM_FORMAT_WRONG.message("提交内容为空")));
        long count = packageChoices.stream().filter(p -> p.checked).count();
        BizEx.trueThrow(count != 1, ErrorEnums.PARAM_FORMAT_WRONG.message("应该只有一个选中项"));
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class PackageChoice {
        /**
         * 套餐id
         */
        @NotNull
        Integer packageId;

        /**
         * 是否选择
         */
        @NotNull
        Boolean checked;
    }

}
