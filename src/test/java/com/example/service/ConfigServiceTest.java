package com.example.service;

import com.example.model.InputType;
import com.example.model.Transformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getTransforers_shouldPopulateAdditionalPropertiesAndInputType() {
        ConfigService configService = new ConfigService(objectMapper);

        List<Transformer> transformers = configService.getTransforers();

        assertFalse(transformers.isEmpty(), "Expected transformers list to be loaded from transformers.json");
        transformers.forEach(transformer -> {
            assertNotNull(
                transformer.getAdditionalProperties(),
                "Expected additionalProperites/additionalProperties to be non-null for transformer: " + transformer.getName()
            );
            assertNotNull(
                transformer.getInputType(),
                "Expected inputType to be non-null for transformer: " + transformer.getName()
            );
        });
        assertEquals(InputType.MULTI, transformers.stream()
            .filter(transformer -> "ConvertMulti".equals(transformer.getName()))
            .findFirst()
            .orElseThrow()
            .getInputType());
    }

    @Test
    void transformer_shouldDeserializeLegacyIsMultipleInputIntoInputType() throws Exception {
        String json = """
            {
              \"name\": \"LegacyTransformer\",
              \"isMultipleInput\": true,
              \"additionalProperties\": {}
            }
            """;

        Transformer transformer = objectMapper.readValue(json, Transformer.class);

        assertEquals(InputType.MULTI, transformer.getInputType());
    }

    @Test
    void transformer_shouldSerializeInputTypeWithoutLegacyIsMultipleInput() throws Exception {
        Transformer transformer = new Transformer();
        transformer.setName("SerializeTransformer");
        transformer.setInputType(InputType.NONE);
        transformer.setAdditionalProperties(java.util.Map.of());

        String json = objectMapper.writeValueAsString(transformer);

        assertTrue(json.contains("\"inputType\":\"NONE\""));
        assertFalse(json.contains("isMultipleInput"));
    }
}
