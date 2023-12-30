package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.enums.Choice;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptionVO {

    Choice choice;
    String desc;

}
