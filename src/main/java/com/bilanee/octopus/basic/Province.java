package com.bilanee.octopus.basic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Province {

    TRANSFER(1, "送电省") {
        @Override
        public Direction interDirection() {
            return Direction.SELL;
        }
    },
    RECEIVER(2, "受电省") {
        @Override
        public Direction interDirection() {
            return Direction.BUY;
        }
    };

    final Integer dbCode;
    final String desc;

    abstract public Direction interDirection();

}
