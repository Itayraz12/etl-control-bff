package com.example.service;

import com.example.model.Deployment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BackendService {

    private final Map<String, List<Deployment>> teamDeployments;
    private final Path configStorageDir;

    public BackendService(@Value("${app.config-storage-dir:src/main/resources/deploymentConfig}") String configStorageDir) {
        this.teamDeployments = initializeTeamDeployments();
        this.configStorageDir = Paths.get(configStorageDir);
    }

    private Map<String, List<Deployment>> initializeTeamDeployments() {
        Map<String, List<Deployment>> teams = new LinkedHashMap<>();
        long now = System.currentTimeMillis();

        // Team A deployments
        List<Deployment> teamADeployments = new ArrayList<>();
        teamADeployments.add(new Deployment("1", "Team A", "Data Pipeline", "GitHub", "draft", "1.0.0", "1.0.0",
            now - 3600 * 1000, now - 86400 * 1000, "staging"));
        teamADeployments.add(new Deployment("2", "Team A", "ETL Job", "Bitbucket", "running", "2.1.3", "2.0.5",
            now - 1800 * 1000, now - 172800 * 1000, "production"));
        teamADeployments.add(new Deployment("4", "Team A", "ETL Job1", "Bitbucket", "running", "2.1.3", "2.0.5",
            now - 1800 * 1000, now - 172800 * 1000, "staging"));
        teamADeployments.add(new Deployment("5", "Team A", "ETL Job2", "Bitbucket", "stopped", "4.1.3", "2.0.5",
            now - 1800 * 1000, now - 172800 * 1000, "staging"));
        teamADeployments.add(new Deployment("3", "Team A", "Analytics", "GitLab", "stopped", "1.5.2", "1.5.2",
            now - 7200 * 1000, now - 259200 * 1000, "production"));
        teamADeployments.add(new Deployment("6", "Team A", "Analytics", "GitLab4", "stopped", "4.5.2", "2.5.2",
            now - 7200 * 1000, now - 259200 * 1000, "production"));
        teams.put("Team A", teamADeployments);

        // Team B deployments
        List<Deployment> teamBDeployments = new ArrayList<>();
        teamBDeployments.add(new Deployment("4", "Team B", "Analytics4", "GitLab", "running", "3.0.1", "2.9.0",
            now - 7200 * 1000, now - 259200 * 1000, "production"));
        teamBDeployments.add(new Deployment("5", "Team B", "Analytics5", "GitLab", "stopped", "1.2.0", "1.2.0",
            now - 7200 * 1000, now - 259200 * 1000, "staging"));
        teamBDeployments.add(new Deployment("6", "Team B", "Analytics6", "GitLab", "draft", "2.0.0", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teams.put("Team B", teamBDeployments);

        // Team C deployments
        List<Deployment> teamCDeployments = new ArrayList<>();
        teamCDeployments.add(new Deployment("7", "Team C", "Analytics7", "GitLab", "draft", "1.3.5", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teamCDeployments.add(new Deployment("8", "Team C", "Analytics8", "GitLab", "draft", "2.2.1", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teamCDeployments.add(new Deployment("9", "Team C", "Analytics9", "GitLab", "draft", "1.1.0", null,
            now - 7200 * 1000, now - 259200 * 1000, "staging"));
        teams.put("Team C", teamCDeployments);

        // Team D deployments
        List<Deployment> teamDDeployments = new ArrayList<>();
        teamDDeployments.add(new Deployment("10", "Team D1", "Analytics10", "GitLab", "draft", "1.7.2", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teamDDeployments.add(new Deployment("11", "Team D1", "Analytics11", "GitLab", "draft", "2.3.0", null,
            now - 7200 * 1000, now - 259200 * 1000, "staging"));
        teamDDeployments.add(new Deployment("12", "Team D1", "Analytics12", "GitLab", "draft", "1.4.8", null,
            now - 7200 * 1000, now - 259200 * 1000, "production"));
        teams.put("Team D1", teamDDeployments);

        return teams;
    }

    public String testKafkaConnection(String topicName, String environment) {
        validateRequired("topicName", topicName);
        validateRequired("environment", environment);
        return "success";
    }

    public String testRabbitMqConnection(String vhost,
                                         String port,
                                         String queueName,
                                         String ip,
                                         String username,
                                         String password,
                                         String environment) {
        validateRequired("vhost", vhost);
        validateRequired("port", port);
        validateRequired("queueName", queueName);
        validateRequired("ip", ip);
        validateRequired("username", username);
        validateRequired("password", password);
        validateRequired("environment", environment);

        validatePort(port);
        return "success";
    }

    private void validateRequired(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    private void validatePort(String port) {
        try {
            int parsedPort = Integer.parseInt(port);
            if (parsedPort <= 0 || parsedPort > 65535) {
                throw new IllegalArgumentException("port must be between 1 and 65535");
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("port must be a valid integer", ex);
        }
    }

    public String getConfigurationYaml(String productType, String source, String team, String environment, boolean isDraft) {
        String fileName;
        if (isDraft){
            fileName = String.format("%s_%s_%s_%s-draft.yml",
                    sanitize(productType), sanitize(source), sanitize(team), sanitize(environment));
        } else {
            fileName = String.format("%s_%s_%s_%s.yml",
                sanitize(productType), sanitize(source), sanitize(team), sanitize(environment));
        }
        Path targetPath = configStorageDir.resolve(fileName);
        if (!Files.exists(targetPath)) {
            throw new IllegalArgumentException("Configuration file not found: " + fileName);
        }
        try {
            return Files.readString(targetPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read configuration file: " + fileName, ex);
        }
    }

    public void saveConfigurationYaml(String productType, String source, String team, String environment, String configurationYaml) {
        if (configurationYaml == null || configurationYaml.isBlank()) {
            throw new IllegalArgumentException("Configuration YAML must not be empty");
        }

        String fileName = String.format("%s_%s_%s_%s-draft.yml",
            sanitize(productType), sanitize(source), sanitize(team), sanitize(environment));

        try {
            Files.createDirectories(configStorageDir);
            Path targetPath = configStorageDir.resolve(fileName);
            Files.writeString(targetPath, configurationYaml);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save configuration file", ex);
        }
    }

    private String sanitize(String value) {
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    public String getSchemaFromPayload(String payload, String formatType) {
        validateRequired("formatType", formatType);
        String fileName = resolveSchemaFileNameForFormatType(formatType);
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read schema.json", ex);
        }
    }

    public String getSchemaByName(String name) {
        String fileName = resolveSchemaFileNameForEntityName(name);
        ClassPathResource resource = new ClassPathResource(fileName);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Schema file not found: " + fileName);
        }
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read schema file: " + fileName, ex);
        }
    }

    private String resolveSchemaFileNameForFormatType(String formatType) {
        return switch (formatType.trim().toUpperCase(Locale.ROOT)) {
            case "CSV" -> "personSchemaFlat.json";
            case "JSON" -> "schema.json";
            default -> throw new IllegalArgumentException("Unsupported formatType: " + formatType);
        };
    }

    private String resolveSchemaFileNameForEntityName(String schemaType) {
        return switch (schemaType.trim().toUpperCase(Locale.ROOT)) {
            case "PERSONARRAY" -> "personSchemaArray.json";
            case "PERSONOBJECT" -> "schema.json";
            default -> "personSchemaFlat.json";
        };
    }

    public List<Deployment> getDeployments(String teamName) {
        return teamDeployments.getOrDefault(teamName, new ArrayList<>());
    }

    public List<Deployment> getAllDeployments() {
        List<Deployment> deployments = new ArrayList<>();
        teamDeployments.values().forEach(deployments::addAll);
        return deployments;
    }
}
