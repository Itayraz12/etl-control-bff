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
import java.util.HashMap;
import java.util.List;
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
        Map<String, List<Deployment>> teams = new HashMap<>();
        long now = System.currentTimeMillis();

        // Team A deployments
        List<Deployment> teamADeployments = new ArrayList<>();
        teamADeployments.add(new Deployment("1", "Data Pipeline", "GitHub", "draft", "1.0.0", "1.0.0",
            now - 3600 * 1000, now - 86400 * 1000, "staging"));
        teamADeployments.add(new Deployment("2", "ETL Job", "Bitbucket", "running", "2.1.3", "2.0.5",
            now - 1800 * 1000, now - 172800 * 1000, "production"));
        teamADeployments.add(new Deployment("4", "ETL Job1", "Bitbucket", "stopped", "2.1.3", "2.0.5",
            now - 1800 * 1000, now - 172800 * 1000, "staging"));
        teamADeployments.add(new Deployment("5", "ETL Job2", "Bitbucket", "stopped", "2.1.3", "2.0.5",
            now - 1800 * 1000, now - 172800 * 1000, "staging"));
        teamADeployments.add(new Deployment("3", "Analytics", "GitLab", "stopped", "1.5.2", "1.5.2",
            now - 7200 * 1000, now - 259200 * 1000, "production"));
        teams.put("Team A", teamADeployments);

        // Team B deployments
        List<Deployment> teamBDeployments = new ArrayList<>();
        teamBDeployments.add(new Deployment("4", "Analytics4", "GitLab", "running", "3.0.1", "2.9.0",
            now - 7200 * 1000, now - 259200 * 1000, "production"));
        teamBDeployments.add(new Deployment("5", "Analytics5", "GitLab", "stopped", "1.2.0", "1.2.0",
            now - 7200 * 1000, now - 259200 * 1000, "staging"));
        teamBDeployments.add(new Deployment("6", "Analytics6", "GitLab", "draft", "2.0.0", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teams.put("Team B", teamBDeployments);

        // Team C deployments
        List<Deployment> teamCDeployments = new ArrayList<>();
        teamCDeployments.add(new Deployment("7", "Analytics7", "GitLab", "draft", "1.3.5", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teamCDeployments.add(new Deployment("8", "Analytics8", "GitLab", "draft", "2.2.1", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teamCDeployments.add(new Deployment("9", "Analytics9", "GitLab", "draft", "1.1.0", null,
            now - 7200 * 1000, now - 259200 * 1000, "staging"));
        teams.put("Team C", teamCDeployments);

        // Team D deployments
        List<Deployment> teamDDeployments = new ArrayList<>();
        teamDDeployments.add(new Deployment("10", "Analytics10", "GitLab", "draft", "1.7.2", null,
            now - 7200 * 1000, now - 259200 * 1000, "development"));
        teamDDeployments.add(new Deployment("11", "Analytics11", "GitLab", "draft", "2.3.0", null,
            now - 7200 * 1000, now - 259200 * 1000, "staging"));
        teamDDeployments.add(new Deployment("12", "Analytics12", "GitLab", "draft", "1.4.8", null,
            now - 7200 * 1000, now - 259200 * 1000, "production"));
        teams.put("Team D", teamDDeployments);

        return teams;
    }

    public String testKafkaConnection(String topicName, String environment) {
        validateRequired("topicName", topicName);
        validateRequired("environment", environment);
        return "success";
    }

    private void validateRequired(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    public String getConfigurationYaml(String productType, String source, String team, String environment, boolean isDraft) {
        String fileName;
        if (isDraft){
            fileName = String.format("%s_%s_%s_%s-draft.yml",
                    sanitize(productType), sanitize(source), sanitize(team), sanitize(environment));
        }else
        fileName = String.format("%s_%s_%s_%s.yml",
            sanitize(productType), sanitize(source), sanitize(team), sanitize(environment));
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

        String fileName = String.format("%s_%s_%s_%s.yml",
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

    public String getSchemaFromPayload(String payload) {
        String fileName = switch (payload) {
            case "personArray" -> "personSchemaArray.json";
            case "personObject" -> "schema.json";
            default -> "personSchemaFlat.json";
        };
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
        String fileName = switch (name) {
            case "personArray" -> "personSchemaArray.json";
            case "personObject" -> "schema.json";
            default -> "personSchemaFlat.json";
        };
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

    public List<Deployment> getDeployments(String teamName) {
        return teamDeployments.getOrDefault(teamName, new ArrayList<>());
    }
}
