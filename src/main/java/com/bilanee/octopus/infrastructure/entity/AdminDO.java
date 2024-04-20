package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("octopus_admin_do")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDO {

    @TableId(type = IdType.INPUT)
    String userId;
    String userName;
    String password;
    String portrait;

}
