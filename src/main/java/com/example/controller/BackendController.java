package com.example.controller;

import com.example.model.Deployment;
import com.example.service.BackendService;
import com.example.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/backend")
@Tag(name = "Backend Service", description = "Backend service operations")
public class BackendController {

    private static final Logger logger = LoggerFactory.getLogger(BackendController.class);

    @Autowired
    private BackendService backendService;

    @Autowired
    private ConfigService configService;

    @GetMapping("/kafka/test-connection")
    @Operation(summary = "Test Kafka connection by topic name and environment")
    public ResponseEntity<String> testKafkaConnection(
            @RequestParam String topicName,
            @RequestParam String environment) {
        logger.info("Request arrived - GET /api/backend/kafka/test-connection [topicName={}, environment={}]",
            topicName, environment);
        try {
            String response = backendService.testKafkaConnection(topicName, environment);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("error: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/rabbitmq/test-connection")
    @Operation(summary = "Test RabbitMQ connection by queue details and environment")
    public ResponseEntity<String> testRabbitMqConnection(
            @RequestParam String vhost,
            @RequestParam String port,
            @RequestParam String queueName,
            @RequestParam String ip,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String environment) {
        logger.info("Request arrived - GET /api/backend/rabbitmq/test-connection [vhost={}, port={}, queueName={}, ip={}, username={}, password={}, environment={}]",
            vhost, port, queueName, ip, username, maskPassword(password), environment);
        try {
            String response = backendService.testRabbitMqConnection(vhost, port, queueName, ip, username, password, environment);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("error: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/teamNames")
    @Operation(summary = "Get supported team names - reads teamNams.txt from classpath")
    public List<String> getTeamNames() {
        logger.info("Request arrived - GET /api/backend/teamNames");
        try {
            List<String> teams = configService.getTeamNames();
            logger.info("Response payload: {} team names returned", teams.size());
            return teams;
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }

    @GetMapping("/deployments")
    @Operation(summary = "Get deployments for a specific team or for all teams when teamName is omitted")
    public List<Deployment> getDeployments(@RequestParam(required = false) String teamName) {
        boolean allTeamsRequested = teamName == null || teamName.isBlank();
        logger.info("Request arrived - GET /api/backend/deployments [teamName={}]",
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
        logger.info("Request arrived - GET /api/backend/configuration/yaml [productType={}, source={}, team={}, environment={}]",
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
        logger.info("Request arrived - GET /api/backend/configuration/draft/yaml [productType={}, source={}, team={}, environment={}]",
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

    @PostMapping(value = "/configuration/yaml", consumes = {"text/plain", "application/x-yaml", "application/json", "application/octet-stream"})
    @Operation(summary = "Save configuration from YAML string. File is saved under deploymentConfig/<productType>_<source>_<team>_<environment>.yml")
    public ResponseEntity<String> saveConfigurationYaml(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment,
            @RequestBody String configurationYaml) {
        logger.info("Request arrived - POST /api/backend/configuration/yaml [productType={}, source={}, team={}, environment={}]",
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

    @GetMapping(value = "/schema/entity/{entityName}", produces = {"application/json"})
    @Operation(summary = "Get schema by entity name - reads <entityName>.json from classpath and returns it")
    public ResponseEntity<String> getSchemaByEntityName(@PathVariable String entityName) {
        logger.info("Request arrived - GET /api/backend/schema/entity/{}", entityName);
        try {
            String schema = backendService.getSchemaByName(entityName);
            logger.info("Response payload: schema '{}' returned ({} chars)", entityName, schema.length());
            return ResponseEntity.ok(schema);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }

    @PostMapping(value = "/schemaByExample/{formatType}", consumes = {"text/plain", "application/json", "application/x-yaml", "application/octet-stream"})
    @Operation(summary = "Get schema from payload using a formatType path value such as CSV or JSON")
    public ResponseEntity<String> getSchemaFromPayload(
            @PathVariable String formatType,
            @RequestBody String payload) {
        logger.info("Request arrived - POST /api/backend/schemaByExample/{}", formatType);
        try {
            String schema = backendService.getSchemaFromPayload(payload, formatType);
            logger.info("Response payload: schema returned ({} chars)", schema.length());
            return ResponseEntity.ok(schema);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("error: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }

    private String maskPassword(String password) {
        if (password == null || password.isBlank()) {
            return "<empty>";
        }
        return "*".repeat(Math.min(password.length(), 8));
    }
}
