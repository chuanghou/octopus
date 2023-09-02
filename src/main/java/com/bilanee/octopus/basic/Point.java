package com.bilanee.octopus.basic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class Point<T> {
    T x;
    T y;
}
