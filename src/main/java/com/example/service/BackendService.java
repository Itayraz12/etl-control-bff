package com.example.service;

import com.example.model.Configuration;
import com.example.model.Deployment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private final ObjectMapper yamlMapper;
    private final Path configStorageDir;

    public BackendService(@Value("${app.config-storage-dir:saved-configurations}") String configStorageDir) {
        this.teamDeployments = initializeTeamDeployments();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configStorageDir = Paths.get(configStorageDir);
    }

    private Map<String, List<Deployment>> initializeTeamDeployments() {
        Map<String, List<Deployment>> teams = new HashMap<>();
        long now = System.currentTimeMillis();

        // Team A deployments
        List<Deployment> teamADeployments = new ArrayList<>();
        teamADeployments.add(new Deployment("1", "Data Pipeline", "GitHub", "draft", "1.0.0", "1.0.0",
            now - 3600 * 1000, now - 86400 * 1000));
        teamADeployments.add(new Deployment("2", "ETL Job", "Bitbucket", "running", "2.1.3", "2.0.5",
            now - 1800 * 1000, now - 172800 * 1000));
        teamADeployments.add(new Deployment("3", "Analytics", "GitLab", "stopped", "1.5.2", "1.5.2",
            now - 7200 * 1000, now - 259200 * 1000));
        teams.put("Team A", teamADeployments);

        // Team B deployments
        List<Deployment> teamBDeployments = new ArrayList<>();
        teamBDeployments.add(new Deployment("4", "Analytics4", "GitLab", "running", "3.0.1", "2.9.0",
            now - 7200 * 1000, now - 259200 * 1000));
        teamBDeployments.add(new Deployment("5", "Analytics5", "GitLab", "stopped", "1.2.0", "1.2.0",
            now - 7200 * 1000, now - 259200 * 1000));
        teamBDeployments.add(new Deployment("6", "Analytics6", "GitLab", "draft", "2.0.0", null,
            now - 7200 * 1000, now - 259200 * 1000));
        teams.put("Team B", teamBDeployments);

        // Team C deployments
        List<Deployment> teamCDeployments = new ArrayList<>();
        teamCDeployments.add(new Deployment("7", "Analytics7", "GitLab", "draft", "1.3.5", null,
            now - 7200 * 1000, now - 259200 * 1000));
        teamCDeployments.add(new Deployment("8", "Analytics8", "GitLab", "draft", "2.2.1", null,
            now - 7200 * 1000, now - 259200 * 1000));
        teamCDeployments.add(new Deployment("9", "Analytics9", "GitLab", "draft", "1.1.0", null,
            now - 7200 * 1000, now - 259200 * 1000));
        teams.put("Team C", teamCDeployments);

        // Team D deployments
        List<Deployment> teamDDeployments = new ArrayList<>();
        teamDDeployments.add(new Deployment("10", "Analytics10", "GitLab", "draft", "1.7.2", null,
            now - 7200 * 1000, now - 259200 * 1000));
        teamDDeployments.add(new Deployment("11", "Analytics11", "GitLab", "draft", "2.3.0", null,
            now - 7200 * 1000, now - 259200 * 1000));
        teamDDeployments.add(new Deployment("12", "Analytics12", "GitLab", "draft", "1.4.8", null,
            now - 7200 * 1000, now - 259200 * 1000));
        teams.put("Team D", teamDDeployments);

        return teams;
    }

    public Configuration saveConfiguration(Configuration config) {
        config.setId("config-" + System.currentTimeMillis());
        return config;
    }

    public Configuration getConfiguration(String id) {
        return new Configuration(id, "Sample Config", "key=value");
    }

    public Configuration saveConfigurationYaml(String configurationYaml) {
        if (configurationYaml == null || configurationYaml.isBlank()) {
            throw new IllegalArgumentException("Configuration YAML must not be empty");
        }

        final Configuration parsedConfiguration;
        try {
            parsedConfiguration = yamlMapper.readValue(configurationYaml, Configuration.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid configuration YAML", ex);
        }

        parsedConfiguration.setId("config-" + System.currentTimeMillis());

        try {
            Files.createDirectories(configStorageDir);
            Path targetPath = configStorageDir.resolve(parsedConfiguration.getId() + ".yml");
            yamlMapper.writeValue(targetPath.toFile(), parsedConfiguration);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save configuration file", ex);
        }

        return parsedConfiguration;
    }

    public List<Deployment> getDeployments(String teamName) {
        if (teamName== null || teamName.isEmpty())
            teamName="Team B";
        return teamDeployments.getOrDefault(teamName, new ArrayList<>());
    }
}

