package com.example.controller;

import com.example.model.DeployResponse;
import com.example.model.DeploymentStep;
import com.example.service.DeployProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/backend/deployments")
@Tag(name = "Deployment Progress", description = "Real-time deployment progress via SSE")
public class DeploymentController {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentController.class);

    @Autowired
    private DeployProgressService deployProgressService;

    // ------------------------------------------------------------------
    // GET /api/backend/deployments/steps
    // ------------------------------------------------------------------

    @GetMapping("/steps")
    @Operation(summary = "Get ordered deployment steps shown in the progress modal")
    public List<DeploymentStep> getSteps() {
        logger.info("Request arrived - GET /api/backend/deployments/steps");
        List<DeploymentStep> steps = deployProgressService.getSteps();
        logger.info("Response payload: {} steps returned", steps.size());
        return steps;
    }

    // ------------------------------------------------------------------
    // POST /api/backend/deployments/{id}/deploy
    // ------------------------------------------------------------------

    @PostMapping(value = "/deploy", consumes = {"text/plain", "application/x-yaml", "application/json", "application/octet-stream"})
    @Operation(summary = "Start a deployment run; returns a deploymentId for the SSE progress stream")
    public ResponseEntity<DeployResponse> deploy(
            @RequestBody String configurationYaml) {
        logger.info("Request arrived - POST /api/backend/deployments/deploy");
        String runId = deployProgressService.startDeployment(configurationYaml);
        logger.info("Deployment initiated: runId={}", runId);
        return ResponseEntity.ok(new DeployResponse(true, runId, runId));
    }

    // ------------------------------------------------------------------
    // GET /api/backend/deployments/{deploymentId}/progress  (SSE)
    // ------------------------------------------------------------------

    @GetMapping(value = "/{deploymentId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream of real-time step progress for a deployment run")
    public SseEmitter streamProgress(@PathVariable String deploymentId) {
        logger.info("Request arrived - GET /api/backend/deployments/{}/progress", deploymentId);
        SseEmitter emitter = new SseEmitter(300_000L); // 5-minute timeout
        deployProgressService.registerEmitter(deploymentId, emitter);
        return emitter;
    }
}

