package com.example.controller;

import com.example.model.Entity;
import com.example.service.BackboneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genome")
@Tag(name = "Backbone Service", description = "Backbone service operations")
public class BackboneController {

    private static final Logger logger = LoggerFactory.getLogger(BackboneController.class);

    @Autowired
    private BackboneService backboneService;

    @GetMapping("/schema/{id}")
    @Operation(summary = "Get entity by ID")
    public Entity getEntity(@PathVariable String id) {
        logger.info("Request arrived - GET /api/genome/schema/{}", id);
        Entity entity = backboneService.getEntity(id);
        logger.info("Response payload: entity returned for id={}", id);
        logger.debug("Response details: {}", entity);
        return entity;
    }

    @GetMapping("/getGenomeEntityList")
    @Operation(summary = "Get all entity list")
    public List<Entity> getEntityList(@RequestParam String environment) {
        logger.info("Request arrived - GET /api/genome/getGenomeEntityList [environment={}]", environment);
        List<Entity> entities = backboneService.getEntity();
        if (environment.equalsIgnoreCase("prod")) {
            entities.add(new Entity("eyal", "eyal","Unknown", "UnknownType"));
        }
        logger.info("Response payload: {} entities returned", entities.size());
        logger.debug("Response details: {}", entities);
        return entities;
    }
}

