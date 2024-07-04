package com.bilanee.octopus.adapter.facade;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RollCancelPO {

    /**
     * 系统阶段id
     */
    @NotBlank
    String stageId;

    /**
     * 待取消的量价单Id
     */
    @NotNull
    Long bidId;

}
