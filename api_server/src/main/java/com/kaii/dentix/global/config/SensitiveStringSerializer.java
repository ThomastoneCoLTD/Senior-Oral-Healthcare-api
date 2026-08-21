package com.kaii.dentix.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class SensitiveStringSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeString("********");
    }
}
