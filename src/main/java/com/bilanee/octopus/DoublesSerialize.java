package com.bilanee.octopus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DoublesSerialize extends JsonSerializer<List<Double>> {

    @Override
    public void serialize(List<Double> values, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(values != null) {
            List<Double> doubles = values.stream().map(v -> {
                if (v != null) {
                    return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
                } else {
                    return null;
                }
            }).collect(Collectors.toList());
            double[] vs = new double[doubles.size()];
            IntStream.range(0, doubles.size()).forEach(i -> vs[i] = doubles.get(i));
            jsonGenerator.writeArray(vs, 0, vs.length);
        }
    }

}