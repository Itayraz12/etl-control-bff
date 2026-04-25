package com.example.controller;

import com.example.model.Deployment;
import com.example.model.FilterEvaluationRequest;
import com.example.model.FilterEvaluationResponse;
import com.example.service.BackendService;
import com.example.service.ConfigService;
import com.example.service.FilterEvaluationService;
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
@RequestMapping("/api")
@Tag(name = "Backend Service", description = "Backend service operations")
public class BackendController {

    private static final Logger logger = LoggerFactory.getLogger(BackendController.class);

    @Autowired
    private BackendService backendService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private FilterEvaluationService filterEvaluationService;

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
    @Operation(summary = "Get supported team names - reads teamNames.txt from classpath")
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




    @GetMapping(value = "/genome/schema/{entityName}", produces = {"application/json"})
    @Operation(summary = "Get schema by entity name - reads <entityName>.json from classpath and returns it")
    public ResponseEntity<String> getSchemaByEntityName(@PathVariable String entityName) {
        logger.info("Request arrived - GET /api/genome/schema/{}", entityName);
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

    @PostMapping(value = "/filters/evaluate", consumes = {"application/json"}, produces = {"application/json"})
    @Operation(summary = "Evaluate a filter configuration against input field values and return whether it matches")
    public ResponseEntity<FilterEvaluationResponse> evaluateFilters(@RequestBody FilterEvaluationRequest request) {
        logger.info("Request arrived - POST /api/backend/filters/evaluate");
        try {
            boolean matches = filterEvaluationService.evaluate(request.getConfiguration(), request.getInputFields());
            logger.info("Response payload: filter evaluation returned {}", matches);
            return ResponseEntity.ok(new FilterEvaluationResponse(matches));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new FilterEvaluationResponse(false));
        }
    }

    private String maskPassword(String password) {
        if (password == null || password.isBlank()) {
            return "<empty>";
        }
        return "*".repeat(Math.min(password.length(), 8));
    }
}
