package com.example.service.filter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
public class SmallerFieldFilterOperator implements FieldFilterOperator {

    @Override
    public Set<String> supportedOperators() {
        return Set.of("SMALLER", "LT", "smaller");
    }

    @Override
    public boolean matches(String inputField, List<String> values) {
        BigDecimal actualValue = parseNumber(inputField);
        return values.stream()
            .map(this::parseNumber)
            .anyMatch(expectedValue -> actualValue.compareTo(expectedValue) < 0);
    }

    private BigDecimal parseNumber(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("SMALLER operator requires numeric values", ex);
        }
    }
}
