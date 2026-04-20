package com.example.service;

import com.example.model.ConfigOption;
import com.example.model.Filter;
import com.example.model.RecordsPerDay;
import com.example.model.StreamingContinuity;
import com.example.model.Transformer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ConfigService {

    private final ObjectMapper objectMapper;

    public ConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Transformer> getTransforers() {
        return readListFromResource("transformers.json", new TypeReference<>() {});
    }

    public List<Filter> getFilters(String environment) {
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("environment must not be empty");
        }
        return readListFromResource("filters.json", new TypeReference<>() {});
    }

    public List<String> getTeamNames() {
        return readLinesFromResource("teamNams.txt");
    }

    public List<ConfigOption> getStreamingContinuities() {
        return Stream.of(StreamingContinuity.values())
            .map(option -> new ConfigOption(option.getValue(), option.getLabel()))
            .toList();
    }

    public List<ConfigOption> getRecordsPerDayOptions() {
        return Stream.of(RecordsPerDay.values())
            .map(option -> new ConfigOption(option.getValue(), option.getLabel()))
            .toList();
    }

    private <T> List<T> readListFromResource(String resourceName, TypeReference<List<T>> typeReference) {
        try (InputStream inputStream = new ClassPathResource(resourceName).getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config file: " + resourceName, e);
        }
    }

    private List<String> readLinesFromResource(String resourceName) {
        try (InputStream inputStream = new ClassPathResource(resourceName).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config file: " + resourceName, e);
        }
    }
}
