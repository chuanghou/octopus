package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.enums.Province;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RollSymbol {

    Province province;
    Integer instant;

    public static List<RollSymbol> rollSymbols() {
        return Arrays.stream(Province.values())
                .flatMap(p -> IntStream.range(0, 24).mapToObj(t -> new RollSymbol(p, t))).collect(Collectors.toList());
    }

}
