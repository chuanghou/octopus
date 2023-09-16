package com.bilanee.octopus.basic.enums;

import com.stellariver.milky.common.tool.util.Collect;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public enum BidStatus {

    NEW_DECELERATED("新单已报") {
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
    CANCELLED("撤单");

    final String desc;

    @SuppressWarnings("unchecked")
    public List<Operation> operations() {
        return Collections.EMPTY_LIST;
    }
}
