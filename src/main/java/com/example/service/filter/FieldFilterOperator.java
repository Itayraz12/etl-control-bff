package com.example.service.filter;

import java.util.Set;

public interface FieldFilterOperator {

    Set<String> supportedOperators();

    boolean matches(String inputField, java.util.List<String> values);
}
