package com.example.controller;

import com.example.model.DeployResponse;
import com.example.model.Deployment;
import com.example.model.DeploymentStep;
import com.example.service.BackendService;
import com.example.service.DeployProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/deployment/")
@Tag(name = "Deployment Progress", description = "Real-time deployment progress via SSE")
public class DeploymentController {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentController.class);
    @Autowired
    private BackendService backendService;
    @Autowired
    private DeployProgressService deployProgressService;
    @PostMapping(value = "/configuration/yaml", consumes = {"text/plain", "application/x-yaml", "application/json", "application/octet-stream"})
    @Operation(summary = "Save configuration from YAML string. File is saved under deploymentConfig/<productType>_<source>_<team>_<environment>.yml")
    public ResponseEntity<String> saveConfigurationYaml(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment,
            @RequestBody String configurationYaml) {
        logger.info("Request arrived - POST /api/deployment/configuration/yaml [productType={}, source={}, team={}, environment={}]",
                productType, source, team, environment);
        try {
            backendService.saveConfigurationYaml(productType, source, team, environment, configurationYaml);
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("error: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/deployments")
    @Operation(summary = "Get deployments for a specific team or for all teams when teamName is omitted")
    public List<Deployment> getDeployments(@RequestParam(required = false) String teamName) {
        boolean allTeamsRequested = teamName == null || teamName.isBlank() || teamName.contains("yarden");
        logger.info("Request arrived - GET /api/deployment/deployments [teamName={}]",
                allTeamsRequested ? "ALL" : teamName);
        List<Deployment> response = allTeamsRequested
                ? backendService.getAllDeployments()
                : backendService.getDeployments(teamName);
        logger.info("Response payload: {} deployments returned", response.size());
        logger.debug("Response details: {}", response);
        return response;
    }


    @GetMapping(value = "/configuration/yaml", produces = {"application/json", "text/plain"})
    @Operation(summary = "Get configuration YAML by productType, source, team and environment")
    public ResponseEntity<String> getConfigurationYaml(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment) {
        logger.info("Request arrived - GET /api/deployment/configuration/yaml [productType={}, source={}, team={}, environment={}]",
                productType, source, team, environment);
        try {
            String yaml = backendService.getConfigurationYaml(productType, source, team, environment, false);
            logger.info("Response payload: YAML content returned ({} chars)", yaml.length());
            return ResponseEntity.ok(yaml);
        } catch (IllegalArgumentException ex) {
            logger.error("Error occurred while getting configuration YAML", ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("eyal is here");
        } catch (IllegalStateException ex) {
            logger.error("Error occurred while getting configuration YAML", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("eyal is here");
        }
    }

    @GetMapping(value = "/configuration/draft/yaml", produces = {"application/json", "text/plain"})
    @Operation(summary = "Get configuration YAML by productType, source, team and environment")
    public ResponseEntity<String> getDraftConfigurationYaml(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment) {
        logger.info("Request arrived - GET /api/deployment/configuration/draft/yaml [productType={}, source={}, team={}, environment={}]",
                productType, source, team, environment);
        try {
            String yaml = backendService.getConfigurationYaml(productType, source, team, environment,true);
            logger.info("Response payload: YAML content returned ({} chars)", yaml.length());
            return ResponseEntity.ok(yaml);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }
    @DeleteMapping(value = "/delete")
    @Operation(summary = "Delete a deployment")
    public ResponseEntity<DeployResponse> delete(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment,
            @RequestParam boolean isPermanent) {
        logger.info("Request arrived - DELETE /api/backend/deployments/delete [productType={}, source={}, team={}, environment={}, isPermanent={}]",
                productType, source, team, environment, isPermanent);
        String runId = deployProgressService.deleteDeployment(productType, source, team, environment, isPermanent);
        logger.info("Delete initiated: runId={}", runId);
        return ResponseEntity.ok(new DeployResponse(true, runId, runId));
    }
    @PostMapping(value = "/restore")
    @Operation(summary = "Delete a deployment")
    public ResponseEntity<DeployResponse> restore(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment) {
        logger.info("Request arrived - DELETE /api/backend/deployments/delete [productType={}, source={}, team={}, environment={}, isPermanent={}]",
                productType, source, team, environment);
        String runId = deployProgressService.restoreDeployment(productType, source, team, environment);
        logger.info("Delete initiated: runId={}", runId);
        return ResponseEntity.ok(new DeployResponse(true, runId, runId));
    }



}

