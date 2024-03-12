package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("node_basic")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NodeBasicDO {
    @TableId(type = IdType.INPUT)
    Integer nodeId;
    String nodeName;
    Integer prov;
    Integer isOffline;
    Integer subregionId;
}
