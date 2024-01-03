package com.bilanee.octopus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.text.DecimalFormat;

public class DoubleSerialize extends JsonSerializer<Double> {

    private final DecimalFormat df = new DecimalFormat("0.00");

    @Override
    public void serialize(Double value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(value != null ) {
            jsonGenerator.writeString(String.valueOf(Double.parseDouble(String.format("%.2f", value))));
        }
    }


}