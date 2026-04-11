package com.example.config;

import com.example.controller.AuthController;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;

@Configuration
public class OpenApiConfig {

    @Bean
    public OperationCustomizer userIdHeaderCustomizer() {
        return this::addUserIdHeader;
    }

    private Operation addUserIdHeader(Operation operation, HandlerMethod handlerMethod) {
        if (AuthController.class.equals(handlerMethod.getBeanType())) {
            return operation;
        }

        boolean alreadyDefined = operation.getParameters() != null
            && operation.getParameters().stream()
            .anyMatch(parameter -> UserIdHeaderFilter.HEADER_NAME.equalsIgnoreCase(parameter.getName())
                && "header".equalsIgnoreCase(parameter.getIn()));

        if (!alreadyDefined) {
            if (operation.getParameters() == null) {
                operation.setParameters(new ArrayList<>());
            }

            operation.addParametersItem(new Parameter()
                .in("header")
                .name(UserIdHeaderFilter.HEADER_NAME)
                .required(true)
                .description("User id propagated from the UI"));
        }

        return operation;
    }
}
