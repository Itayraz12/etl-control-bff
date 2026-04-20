package com.example.service;

import com.example.model.ConfigOption;
import com.example.model.Filter;
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

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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

    @Test
    void getFilters_shouldLoadFiltersForProvidedEnvironment() {
        ConfigService configService = new ConfigService(objectMapper);

        List<Filter> filters = configService.getFilters("prod");

        assertFalse(filters.isEmpty(), "Expected filters list to be loaded from filters.json");
        assertEquals("equals", filters.get(0).getName());
    }

    @Test
    void getStreamingContinuities_shouldReturnOrderedValueLabelOptions() {
        ConfigService configService = new ConfigService(objectMapper);

        List<ConfigOption> options = configService.getStreamingContinuities();

        assertEquals(List.of(
            new ConfigOption("once", "Once"),
            new ConfigOption("every-hour", "Every Hour"),
            new ConfigOption("every-few-hours", "Every Few Hours"),
            new ConfigOption("every-day", "Once a Day"),
            new ConfigOption("continuous", "Continuous")
        ), options);
    }

    @Test
    void getRecordsPerDayOptions_shouldReturnOrderedValueLabelOptions() {
        ConfigService configService = new ConfigService(objectMapper);

        List<ConfigOption> options = configService.getRecordsPerDayOptions();

        assertEquals(List.of(
            new ConfigOption("hundreds", "Hundreds"),
            new ConfigOption("thousands", "Thousands"),
            new ConfigOption("hun-thousands", "Hundred of Thousands"),
            new ConfigOption("millions", "A Few Millions"),
            new ConfigOption("tens-millions", "Tens of Millions"),
            new ConfigOption("hundreds-millions", "Hundreds of Millions")
        ), options);
    }
}
