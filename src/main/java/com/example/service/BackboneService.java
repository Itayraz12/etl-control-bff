package com.example.service;

import com.example.model.Entity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class BackboneService {

    private final ObjectMapper objectMapper;

    public BackboneService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Entity getEntity(String id) {
        return readEntitiesFromResource().stream()
            .filter(entity -> entity.getId().equals(id))
            .findFirst()
            .orElseGet(() -> new Entity(id, "Unknown", "UnknownType", "Entity not found in entity.json"));
    }
    public List<Entity> getEntity() {
        return readEntitiesFromResource();
    }

    private List<Entity> readEntitiesFromResource() {
        try (InputStream inputStream = new ClassPathResource("entity.json").getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read entity.json", e);
        }
    }
}
