package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.infrastructure.entity.Question;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaperVO {

    Integer id;
    String name;
    List<Question> questions;

}
