package com.example.service;

import com.example.model.Entity;
import org.springframework.stereotype.Service;

@Service
public class BackboneService {

    public Entity getEntity(String id) {
        return new Entity(id, "EntityType", "Sample entity description");
    }
}

