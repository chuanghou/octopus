package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UnitType {

    GENERATOR(1,"机组") {
        @Override
        public Direction generalDirection() {
            return Direction.SELL;
        }
    }, LOAD(2, "负荷") {
        @Override
        public Direction generalDirection() {
            return Direction.BUY;
        }
    };

    final Integer dbCode;
    final String desc;

    abstract public Direction generalDirection();
}
