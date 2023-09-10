package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.demo.Section;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraMarketRealtimeDO {


    @TableId(type = IdType.AUTO)
    Long id;
    String stageId;
    Province province;
    TimeFrame timeFrame;
    Double price;
    List<Ask> buyAsks;
    List<Ask> sellAsks;
    List<Double> buySections;
    List<Double> sellSections;


}
