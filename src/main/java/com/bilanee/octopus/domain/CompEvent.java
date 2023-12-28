package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.basic.StageId;
import com.stellariver.milky.domain.support.event.Event;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CompEvent {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Created extends Event {

        Comp comp;
        List<Map<String, Collection<MetaUnit>>> roundMetaUnits;

        @Override
        public String getAggregateId() {
            return comp.getCompId().toString();
        }

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Stepped extends Event {

        Long compId;

        StageId last;
        StageId now;

        @Override
        public String getAggregateId() {
            return compId.toString();
        }

    }
}
