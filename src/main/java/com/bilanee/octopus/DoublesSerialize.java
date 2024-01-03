package com.bilanee.octopus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.SneakyThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DoublesSerialize extends JsonSerializer<List<Double>> {

    @Override
    @SneakyThrows
    public void serialize(List<Double> values, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(values != null) {
            jsonGenerator.writeStartArray();
            for (Double value : values) {
                if (value != null) {
                    jsonGenerator.writeNumber(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue());
                } else {
                    jsonGenerator.writeNull();
                }
            }
            jsonGenerator.writeEndArray();
        } else {
            jsonGenerator.writeNull();
        }
    }

}