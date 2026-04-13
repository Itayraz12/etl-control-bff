package com.example.service.filter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class EqualsFieldFilterOperator implements FieldFilterOperator {

    @Override
    public Set<String> supportedOperators() {
        return Set.of("EQUALS", "EQUAL", "EQ", "F-2", "equals");
    }

    @Override
    public boolean matches(String inputField, List<String> values) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(inputField));
    }
}
