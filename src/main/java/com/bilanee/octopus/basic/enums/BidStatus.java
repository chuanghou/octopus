package com.bilanee.octopus.basic.enums;

import com.bilanee.octopus.basic.Deal;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public enum BidStatus {

    NEW_DECELERATED("尚未成交") {
        @Override
        public InstantStatus instantStatus(List<Deal> deals) {
            return InstantStatus.NOT_DEALT;
        }

        @Override
        public List<Operation> operations() {
            return Collect.asList(Operation.CANCEL);
        }
    },
    PART_DEAL("部分成交") {
        @Override
        public InstantStatus instantStatus(List<Deal> deals) {
            return InstantStatus.PART_DEALT;
        }

        @Override
        public List<Operation> operations() {
            return Collect.asList(Operation.CANCEL);
        }
    },
    COMPLETE_DEAL("全部成交") {
        @Override
        public InstantStatus instantStatus(List<Deal> deals) {
            return InstantStatus.ALL_DEALT;
        }
    },
    MANUAL_CANCELLED("手动撤单") {
        @Override
        public InstantStatus instantStatus(List<Deal> deals) {

            return Collect.isEmpty(deals) ? InstantStatus.NOT_BID : InstantStatus.PART_DEALT_PART_CANCELLED;
        }
    },
    SYSTEM_CANCELLED("系统撤单") {
        @Override
        public InstantStatus instantStatus(List<Deal> deals) {
            return Collect.isEmpty(deals) ? InstantStatus.SYSTEM_CANCELLED : InstantStatus.PART_DEALT_PART_CANCELLED;
        }
    };

    final String desc;

    abstract public InstantStatus instantStatus(List<Deal> deals);

    @SuppressWarnings("unchecked")
    public List<Operation> operations() {
        return Collections.EMPTY_LIST;
    }
}
