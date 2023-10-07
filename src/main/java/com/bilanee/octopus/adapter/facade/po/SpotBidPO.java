package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotBidPO {

    /**
     * 阶段id
     */
    @NotBlank
    String stageId;

    /**
     * 时刻
     */
    @NotNull @Min(0) @Max(23)
    Integer instant;

    /**
     * 数量
     */
    @NotNull @Positive
    Double quantity;

    /**
     * 价格
     */
    @NotNull @Positive
    Double price;


}
