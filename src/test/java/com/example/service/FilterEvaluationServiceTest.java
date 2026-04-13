package com.example.service;

import com.example.model.FilterConfiguration;
import com.example.service.filter.EqualsFieldFilterOperator;
import com.example.service.filter.GreaterFieldFilterOperator;
import com.example.service.filter.NotEqualFieldFilterOperator;
import com.example.service.filter.SmallerFieldFilterOperator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterEvaluationServiceTest {

    private final FilterEvaluationService filterEvaluationService = new FilterEvaluationService(List.of(
        new EqualsFieldFilterOperator(),
        new NotEqualFieldFilterOperator(),
        new GreaterFieldFilterOperator(),
        new SmallerFieldFilterOperator()
    ));

    @Test
    void evaluate_shouldReturnTrueForNestedAndOrRule() {
        boolean matches = filterEvaluationService.evaluate(configurationWithNestedRule(), Map.of(
            "firstName", "john",
            "lastName", "cleease",
            "age", "30"
        ));

        assertTrue(matches);
    }

    @Test
    void evaluate_shouldReturnFalseWhenOneAndConditionDoesNotMatch() {
        boolean matches = filterEvaluationService.evaluate(configurationWithNestedRule(), Map.of(
            "firstName", "john",
            "lastName", "other",
            "age", "30"
        ));

        assertFalse(matches);
    }

    @Test
    void evaluate_shouldRejectUnsupportedOperator() {
        FilterConfiguration configuration = configurationWithNestedRule();
        configuration.getFilters().getConfig().get(0).getRule().getAnd().get(0).setOp("UNKNOWN");

        assertThrows(IllegalArgumentException.class,
            () -> filterEvaluationService.evaluate(configuration, Map.of("firstName", "john", "lastName", "cleease", "age", "30")));
    }

    private FilterConfiguration configurationWithNestedRule() {
        return new FilterConfiguration(
            new FilterConfiguration.FiltersDefinition(
                List.of(
                    new FilterConfiguration.FilterDependency("EQUALS"),
                    new FilterConfiguration.FilterDependency("GREATER"),
                    new FilterConfiguration.FilterDependency("SMALLER")
                ),
                List.of(
                    new FilterConfiguration.FilterConfigEntry(
                        new FilterConfiguration.FilterRuleGroup(
                            List.of(
                                new FilterConfiguration.FilterRuleNode("firstName", "EQUALS", List.of("john"), null),
                                new FilterConfiguration.FilterRuleNode("lastName", "EQUALS", List.of("cleease"), null),
                                new FilterConfiguration.FilterRuleNode(null, null, null,
                                    new FilterConfiguration.FilterRuleGroup(
                                        null,
                                        List.of(
                                            new FilterConfiguration.FilterRuleNode("age", "GREATER", List.of("20"), null),
                                            new FilterConfiguration.FilterRuleNode("age", "SMALLER", List.of("50"), null)
                                        )
                                    ))
                            ),
                            null
                        )
                    )
                )
            )
        );
    }
}
