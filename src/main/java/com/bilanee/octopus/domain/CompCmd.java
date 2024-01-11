package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.StageId;
import com.stellariver.milky.domain.support.command.Command;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;
import java.util.List;

public class CompCmd {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Create extends Command {

        Long compId;
        Long startTimeStamp;
        DelayConfig delayConfig;
        List<String> userIds;
        List<String> traderIds;
        List<String> robotIds;
        Boolean enableQuiz;
        String dt;

        @Override
        public String getAggregateId() {
            return compId.toString();
        }

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Step extends Command {

        StageId stageId;
        @Nullable
        Long duration;

        @Override
        public String getAggregateId() {
            return stageId.getCompId().toString();
        }

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Clear extends Command {

        Long compId;

        @Override
        public String getAggregateId() {
            return compId.toString();
        }

    }
}
