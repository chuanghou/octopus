package com.bilanee.octopus.basic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UnitType {

    GENERATOR("机组") {
        @Override
        public Direction generalDirection() {
            return Direction.SELL;
        }
    }, LOAD("负荷") {
        @Override
        public Direction generalDirection() {
            return Direction.BUY;
        }
    };

    final String desc;

    abstract public Direction generalDirection();
}
