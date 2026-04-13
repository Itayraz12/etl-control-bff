package com.example.service.filter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class NotEqualFieldFilterOperator implements FieldFilterOperator {

    @Override
    public Set<String> supportedOperators() {
        return Set.of("NOT_EQUAL", "NOT EQUAL", "NEQ", "not equal", "not_equal");
    }

    @Override
    public boolean matches(String inputField, List<String> values) {
        return values.stream().noneMatch(value -> value.equalsIgnoreCase(inputField));
    }
}
