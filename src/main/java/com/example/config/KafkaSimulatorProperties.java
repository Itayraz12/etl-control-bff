package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaSimulatorProperties {

    private Map<String, String> environments = new HashMap<>();

    public Map<String, String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Map<String, String> environments) {
        this.environments = environments;
    }

    public String getBroker(String environment) {
        return environments.get(environment);
    }
}

