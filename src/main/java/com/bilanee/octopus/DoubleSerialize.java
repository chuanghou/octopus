package com.bilanee.octopus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DoubleSerialize extends JsonSerializer<Double> {

    @Override
    public void serialize(Double value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(value != null ) {
            jsonGenerator.writeNumber(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue());
        } else {
            jsonGenerator.writeNull();
        }
    }

}