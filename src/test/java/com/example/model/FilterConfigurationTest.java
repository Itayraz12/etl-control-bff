package com.example.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FilterConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Test
    void shouldDeserializeNestedFilterConfigurationYaml() throws Exception {
        String yaml = """
            filters:
              dependencies:
                - type: F-2
                - type: NOT_EQUAL
                - type: GREATER
                - type: SMALLER
              config:
                - rule:
                    and:
                      - field: id
                        op: F-2
                        values:
                          - "1"
                      - field: firstName
                        op: NOT_EQUAL
                        values:
                          - "1"
                      - rule:
                          or:
                            - field: lastName
                              op: GREATER
                              values:
                                - "1"
                            - field: age
                              op: SMALLER
                              values:
                                - "12"
            """;

        FilterConfiguration configuration = objectMapper.readValue(yaml, FilterConfiguration.class);

        assertNotNull(configuration.getFilters());
        assertEquals(4, configuration.getFilters().getDependencies().size());
        assertEquals("F-2", configuration.getFilters().getDependencies().get(0).getType());
        assertEquals("SMALLER", configuration.getFilters().getDependencies().get(3).getType());

        FilterConfiguration.FilterConfigEntry configEntry = configuration.getFilters().getConfig().get(0);
        assertNotNull(configEntry.getRule());
        assertEquals(3, configEntry.getRule().getAnd().size());

        FilterConfiguration.FilterRuleNode firstCondition = configEntry.getRule().getAnd().get(0);
        assertEquals("id", firstCondition.getField());
        assertEquals("F-2", firstCondition.getOp());
        assertEquals("1", firstCondition.getValues().get(0));

        FilterConfiguration.FilterRuleNode nestedRuleNode = configEntry.getRule().getAnd().get(2);
        assertNotNull(nestedRuleNode.getRule());
        assertEquals(2, nestedRuleNode.getRule().getOr().size());

        FilterConfiguration.FilterRuleNode nestedOrCondition = nestedRuleNode.getRule().getOr().get(1);
        assertEquals("age", nestedOrCondition.getField());
        assertEquals("SMALLER", nestedOrCondition.getOp());
        assertEquals("12", nestedOrCondition.getValues().get(0));
    }
}

