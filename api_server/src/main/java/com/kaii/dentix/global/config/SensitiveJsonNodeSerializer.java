package com.kaii.dentix.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class SensitiveJsonNodeSerializer extends JsonSerializer<JsonNode> {

    @Override
    public void serialize(JsonNode value, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeString("********");
    }
}
