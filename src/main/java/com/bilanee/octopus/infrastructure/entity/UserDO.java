package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.UserType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("octopus_user_do")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDO  {

    @TableId(type = IdType.INPUT)
    String userId;
    String userName;
    String password;
    String portrait;
    UserType userType;

    public static void main(String[] args) {
        System.out.println(TokenUtils.sign("1000"));
    }
}
