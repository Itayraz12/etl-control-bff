package com.example.service;

import com.example.model.Transformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigServiceTest {

    @Test
    void getTransforers_shouldPopulateAdditionalProperties() {
        ConfigService configService = new ConfigService(new ObjectMapper());

        List<Transformer> transformers = configService.getTransforers();

        assertFalse(transformers.isEmpty(), "Expected transformers list to be loaded from transformers.json");
        transformers.forEach(transformer ->
            assertNotNull(
                transformer.getAdditionalProperties(),
                "Expected additionalProperites/additionalProperties to be non-null for transformer: " + transformer.getName()
            )
        );
    }
}

