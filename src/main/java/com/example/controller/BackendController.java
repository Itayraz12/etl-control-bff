package com.example.controller;

import com.example.model.Configuration;
import com.example.model.Deployment;
import com.example.service.BackendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/backend")
@Tag(name = "Backend Service", description = "Backend service operations")
public class BackendController {

    private static final Logger logger = LoggerFactory.getLogger(BackendController.class);

    @Autowired
    private BackendService backendService;

    @PostMapping("/configuration")
    @Operation(summary = "Save configuration")
    public Configuration saveConfiguration(@RequestBody Configuration config) {
        logger.info("Request arrived - POST /api/backend/configuration with body: {}", config);
        Configuration response = backendService.saveConfiguration(config);
        logger.info("Response payload: {}", response);
        return response;
    }

    @GetMapping("/configuration/{id}")
    @Operation(summary = "Get configuration by ID")
    public Configuration getConfiguration(@PathVariable String id) {
        logger.info("Request arrived - GET /api/backend/configuration/{} with id: {}", id, id);
        Configuration response = backendService.getConfiguration(id);
        logger.info("Response payload: {}", response);
        return response;
    }

    @GetMapping("/deployments")
    @Operation(summary = "Get deployments by team name")
    public List<Deployment> getDeployments(@RequestParam String teamName) {
        logger.info("Request arrived - GET /api/backend/deployments with teamName: {}", teamName);
        List<Deployment> response = backendService.getDeployments(teamName);
        logger.info("Response payload: {} deployments returned", response.size());
        logger.debug("Response details: {}", response);
        return response;
    }

    @PostMapping(value = "/configuration/yaml", consumes = {"text/plain", "application/x-yaml", "application/yaml"})
    @Operation(summary = "Save configuration from YAML")
    public Configuration saveConfigurationYaml(@RequestBody String configurationYaml) {
        logger.info("Request arrived - POST /api/backend/configuration/yaml");
        try {
            Configuration response = backendService.saveConfigurationYaml(configurationYaml);
            logger.info("Response payload: {}", response);
            return response;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }
}
