package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.CompStage;
import com.bilanee.octopus.basic.MarketStatus;
import com.bilanee.octopus.basic.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserVO {

    /**
     * 用户id
     */
    String userId;

    /**
     * 用户名称
     */
    String userName;

    /**
     * 用户头像
     */
    String portrait;

}
