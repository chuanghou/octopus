package com.bilanee.octopus;

import com.bilanee.octopus.basic.Point;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.stellariver.milky.common.tool.util.Json;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PointSerialize extends JsonSerializer<Point<Double>> {

    @Override
    public void serialize(Point<Double> value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(value != null) {
            Point<Double> point = Point.<Double>builder()
                    .x(BigDecimal.valueOf(value.x).setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .y(BigDecimal.valueOf(value.y).setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .build();
            jsonGenerator.writeRaw(Json.toJson(point));
        } else {
            jsonGenerator.writeNull();
        }
    }

}