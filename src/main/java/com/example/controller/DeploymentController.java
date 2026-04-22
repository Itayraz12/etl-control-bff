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
@RequestMapping("/api/backend")
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
    // POST /api/backend/deployments/deploy
    // ------------------------------------------------------------------

    @PostMapping(value = "/deploy", consumes = {"text/plain", "application/x-yaml", "application/json", "application/octet-stream"})
    @Operation(summary = "Start a deployment run; returns a deploymentId for the SSE progress stream")
    public ResponseEntity<DeployResponse> deploy(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment,
            @RequestParam boolean isDeploy,
            @RequestBody String configurationYaml) {
        logger.info("Request arrived - POST /api/backend/deployments/deploy [productType={}, source={}, team={}, environment={}, isDeploy={}]",
                productType, source, team, environment, isDeploy);
        String runId = deployProgressService.startDeployment(productType, source, team, configurationYaml,isDeploy);
        logger.info("Deployment initiated: runId={}", runId);
        return ResponseEntity.ok(new DeployResponse(true, runId, runId));
    }

    // ------------------------------------------------------------------
    // POST /api/backend/deployments/stop
    // ------------------------------------------------------------------

    @PostMapping(value = "/stop")
    @Operation(summary = "Stop a running deployment")
    public ResponseEntity<DeployResponse> stop(
            @RequestParam String productType,
            @RequestParam String source,
            @RequestParam String team,
            @RequestParam String environment) {
        logger.info("Request arrived - POST /api/backend/deployments/stop [productType={}, source={}, team={}, environment={}]",
                productType, source, team, environment);
        String runId = deployProgressService.stopDeployment(productType, source, team, environment);
        logger.info("Stop initiated: runId={}", runId);
        return ResponseEntity.ok(new DeployResponse(true, runId, runId));
    }

    // ------------------------------------------------------------------
    // DELETE /api/backend/deployments/delete
    // ------------------------------------------------------------------

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

