package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraCancelPO {

    /**
     * 系统阶段id
     */
    @NotBlank
    String stageId;

    /**
     * 待取消的量价单Id
     */
    Long bidId;

}
