package com.bilanee.octopus.domain;

import com.stellariver.milky.domain.support.event.Event;
import lombok.*;
import lombok.experimental.FieldDefaults;


public class UnitEvent {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Created extends Event {

        Unit unit;

        @Override
        public String getAggregateId() {
            return unit.getUnitId().toString();
        }

    }

}
