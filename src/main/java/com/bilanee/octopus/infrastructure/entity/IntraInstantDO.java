package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.bilanee.octopus.basic.Volume;
import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.stellariver.milky.infrastructure.base.database.ListJsonHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@TableName(value = "octopus_intra_instant_do", autoResultMap = true)
public class IntraInstantDO {


    @TableId(type = IdType.AUTO)
    Long id;
    String stageId;
    Province province;
    TimeFrame timeFrame;
    Integer instant;
    Double price;
    @TableField(typeHandler = ListAskJsonHandler.class)
    List<Ask> buyAsks;
    @TableField(typeHandler = ListAskJsonHandler.class)
    List<Ask> sellAsks;
    @TableField(typeHandler = JacksonTypeHandler.class)
    List<Volume> buyVolumes;
    @TableField(typeHandler = JacksonTypeHandler.class)
    List<Volume> sellVolumes;

    static public class ListAskJsonHandler extends ListJsonHandler<Ask> {}
    static public class ListVolumeJsonHandler extends ListJsonHandler<Volume> {}


}
