package com.example.service;

import com.example.model.Filter;
import com.example.model.Transformer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ConfigService {

    private final ObjectMapper objectMapper;

    public ConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Transformer> getTransforers() {
        return readListFromResource("transformers.json", new TypeReference<>() {});
    }

    public List<Filter> getFilters() {
        return readListFromResource("filters.json", new TypeReference<>() {});
    }

    private <T> List<T> readListFromResource(String resourceName, TypeReference<List<T>> typeReference) {
        try (InputStream inputStream = new ClassPathResource(resourceName).getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config file: " + resourceName, e);
        }
    }


}
