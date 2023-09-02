package com.bilanee.octopus.domain;

import com.stellariver.milky.domain.support.base.AggregateRoot;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends AggregateRoot {

    String userId;

    /**
     * 每次竞赛都有可能变化，用来分配相应的单元id
     */
    Integer positionId;
    String password;
    String name;

    @Override
    public String getAggregateId() {
        return userId;
    }

}
