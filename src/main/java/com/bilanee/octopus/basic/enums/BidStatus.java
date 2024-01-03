package com.bilanee.octopus.basic.enums;

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
        public List<Operation> operations() {
            return Collect.asList(Operation.CANCEL);
        }
    },
    PART_DEAL("部分成交") {
        @Override
        public List<Operation> operations() {
            return Collect.asList(Operation.CANCEL);
        }
    },
    COMPLETE_DEAL("全部成交"),
    MANUAL_CANCELLED("手动撤单"),
    SYSTEM_CANCELLED("系统撤单");

    final String desc;

    @SuppressWarnings("unchecked")
    public List<Operation> operations() {
        return Collections.EMPTY_LIST;
    }
}
