package com.example.service;

import com.example.model.FilterConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.example.service.filter.FieldFilterOperator;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FilterEvaluationService {

    private final Map<String, FieldFilterOperator> operators;

    public FilterEvaluationService(List<FieldFilterOperator> filterOperators) {
        this.operators = filterOperators.stream()
            .flatMap(operator -> operator.supportedOperators().stream()
                .map(alias -> Map.entry(normalize(alias), operator)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    public boolean evaluate(FilterConfiguration configuration, Map<String, String> inputFields) {
        if (configuration == null || configuration.getFilters() == null) {
            throw new IllegalArgumentException("Filter configuration must not be empty");
        }
        if (CollectionUtils.isEmpty(configuration.getFilters().getConfig())) {
            throw new IllegalArgumentException("Filter configuration must contain at least one config rule");
        }
        if (inputFields == null || inputFields.isEmpty()) {
            throw new IllegalArgumentException("Input fields must not be empty");
        }

        return configuration.getFilters().getConfig().stream()
            .map(FilterConfiguration.FilterConfigEntry::getRule)
            .allMatch(ruleGroup -> evaluateRuleGroup(ruleGroup, inputFields));
    }

    private boolean evaluateRuleGroup(FilterConfiguration.FilterRuleGroup ruleGroup, Map<String, String> inputFields) {
        if (ruleGroup == null) {
            throw new IllegalArgumentException("Rule group must not be empty");
        }

        boolean hasAndRules = !CollectionUtils.isEmpty(ruleGroup.getAnd());
        boolean hasOrRules = !CollectionUtils.isEmpty(ruleGroup.getOr());
        if (!hasAndRules && !hasOrRules) {
            throw new IllegalArgumentException("Rule group must contain 'and' or 'or' rules");
        }

        boolean andResult = !hasAndRules || ruleGroup.getAnd().stream().allMatch(node -> evaluateNode(node, inputFields));
        boolean orResult = !hasOrRules || ruleGroup.getOr().stream().anyMatch(node -> evaluateNode(node, inputFields));
        return andResult && orResult;
    }

    private boolean evaluateNode(FilterConfiguration.FilterRuleNode node, Map<String, String> inputFields) {
        if (node == null) {
            throw new IllegalArgumentException("Rule node must not be null");
        }
        if (node.getRule() != null) {
            return evaluateRuleGroup(node.getRule(), inputFields);
        }

        if (!StringUtils.hasText(node.getField())) {
            throw new IllegalArgumentException("Rule field must not be empty");
        }
        if (!StringUtils.hasText(node.getType())) {
            throw new IllegalArgumentException("Rule operator must not be empty");
        }
        if (CollectionUtils.isEmpty(node.getValues())) {
            throw new IllegalArgumentException("Rule values must not be empty");
        }

        String actualFieldValue = inputFields.get(node.getField());
        if (!StringUtils.hasText(actualFieldValue)) {
            return false;
        }

        FieldFilterOperator operator = operators.get(normalize(node.getType()));
        if (operator == null) {
            throw new IllegalArgumentException("Unsupported filter operator: " + node.getType());
        }

        return operator.matches(actualFieldValue, node.getValues());
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}

