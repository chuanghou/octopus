package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.enums.Province;
import com.bilanee.octopus.basic.enums.TimeFrame;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraSymbol {

    Province province;
    TimeFrame timeFrame;

    public static List<IntraSymbol> intraSymbols() {
        return Arrays.stream(Province.values())
                .flatMap(p -> Arrays.stream(TimeFrame.values()).map(t -> new IntraSymbol(p, t))).collect(Collectors.toList());
    }

}
