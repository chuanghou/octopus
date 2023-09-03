package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Direction {
    BUY("买") {

        @Override
        public Direction opposite() {
            return SELL;
        }

        @Override
        public boolean across(double value, double base) {
            return value < base;
        }

    }, SELL("卖") {

        @Override
        public Direction opposite() {
            return BUY;
        }

        @Override
        public boolean across(double value, double base) {
            return value > base;
        }

    };

    final String desc;

    public abstract Direction opposite();

    public abstract boolean across(double value, double base);
}
