package com.example.service;

import com.example.model.DeploymentStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class DeployProgressService {

    private static final Logger logger = LoggerFactory.getLogger(DeployProgressService.class);

    /**
     * Sentinel placed on the queue by the background thread once it has finished
     * (success or failure).  The drain loop in registerEmitter uses this to know
     * when to stop.
     */
    private static final Object QUEUE_DONE = new Object();

    // deploymentId → registered SseEmitter (present only after the browser connects)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // deploymentId → buffered events produced before the emitter is registered
    private final Map<String, LinkedBlockingQueue<Object>> eventBuffers = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Step definitions
    // -----------------------------------------------------------------------

    private static final List<DeploymentStep> STEPS = List.of(
        new DeploymentStep("validate-config-eyal",   "BE Validating pipeline configuration"),
        new DeploymentStep("prepare-resources", "BE Preparing Kafka topics"),
        new DeploymentStep("validate-mappings", "BE Validating field mappings"),
        new DeploymentStep("prepare-flink",     "BE Preparing Flink job"),
        new DeploymentStep("upload-artifacts",  "BE Uploading pipeline artifacts"),
        new DeploymentStep("register-pipeline", "BE Registering pipeline"),
        new DeploymentStep("deploy-job",        "BE Deploying Flink job"),
        new DeploymentStep("health-checks",     "BE Running health checks")
    );

    public List<DeploymentStep> getSteps() {
        return STEPS;
    }

    // -----------------------------------------------------------------------
    // Start deployment — called by POST /deploy
    // -----------------------------------------------------------------------

    /**
     * Generates a unique run ID, pre-allocates the event buffer so events
     * produced before the browser registers the emitter are never lost, then
     * fires the async work.
     */
    public String startDeployment(String productType, String source, String team, String configurationYaml) {
        String runId = UUID.randomUUID().toString();
        eventBuffers.put(runId, new LinkedBlockingQueue<>());
        logger.info("Deployment started: runId={}, productType={}, source={}, team={}", runId, productType, source, team);
        CompletableFuture.runAsync(() -> executeDeployment(configurationYaml, runId));
        return runId;
    }
    public String upgradeDeployment(String productType, String source, String team, String configurationYaml) {
        String runId = UUID.randomUUID().toString();
        eventBuffers.put(runId, new LinkedBlockingQueue<>());
        logger.info("Deployment upgraded: runId={}, productType={}, source={}, team={}", runId, productType, source, team);
        CompletableFuture.runAsync(() -> executeDeployment(configurationYaml, runId));
        return runId;
    }

    /**
     * Stops a running deployment for the given productType/source/team.
     * Returns a run ID representing the stop operation.
     */
    public String stopDeployment(String productType, String source, String team, String environment) {
        String runId = UUID.randomUUID().toString();
        logger.info("Stop deployment: runId={}, productType={}, source={}, team={}, environment={}", runId, productType, source, team, environment);
        // TODO: implement actual stop logic (cancel running Flink job, etc.)
        return runId;
    }

    /**
     * Deletes a deployment for the given productType/source/team/environment.
     * Returns a run ID representing the delete operation.
     */
    public String deleteDeployment(String productType, String source, String team, String environment, boolean isPermanent) {
        String runId = UUID.randomUUID().toString();
        logger.info("Delete deployment: runId={}, productType={}, source={}, team={}, environment={}, isPermanent={}", runId, productType, source, team, environment, isPermanent);
        // TODO: implement actual delete logic (remove pipeline registration, clean up resources, etc.)
        return runId;
    }
    /**
     * Deletes a deployment for the given productType/source/team/environment.
     * Returns a run ID representing the delete operation.
     */
    public String restoreDeployment(String productType, String source, String team, String environment) {
        String runId = UUID.randomUUID().toString();
        logger.info("Delete deployment: runId={}, productType={}, source={}, team={}, environment={}, ", runId, productType, source, team, environment);
        // TODO: implement actual delete logic (remove pipeline registration, clean up resources, etc.)
        return runId;
    }

    // -----------------------------------------------------------------------
    // Register emitter — called by GET /progress
    // -----------------------------------------------------------------------

    /**
     * Attaches the browser's SSE emitter and immediately flushes any events that
     * were buffered between the POST /deploy response and now.
     */
    public void registerEmitter(String runId, SseEmitter emitter) {
        emitters.put(runId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(runId);
            eventBuffers.remove(runId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(runId);
            eventBuffers.remove(runId);
        });
        emitter.onError(e -> {
            emitters.remove(runId);
            eventBuffers.remove(runId);
        });

        // Drain events buffered before the emitter arrived
        LinkedBlockingQueue<Object> buffer = eventBuffers.get(runId);
        if (buffer != null) {
            CompletableFuture.runAsync(() -> drainBuffer(runId, buffer, emitter));
        }
    }

    /**
     * Reads buffered events and writes them to the emitter.
     * Stops when it dequeues the QUEUE_DONE sentinel or when the emitter is gone.
     */
    private void drainBuffer(String runId, LinkedBlockingQueue<Object> buffer, SseEmitter emitter) {
        try {
            while (true) {
                Object item = buffer.poll(60, TimeUnit.SECONDS);
                if (item == null || item == QUEUE_DONE) {
                    break;
                }
                if (item instanceof SseEvent evt) {
                    sendToEmitter(emitter, evt.name(), evt.data());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -----------------------------------------------------------------------
    // Async deployment execution
    // -----------------------------------------------------------------------

    private void executeDeployment(String configurationYaml, String runId) {
        try {
            for (int i = 0; i < STEPS.size(); i++) {
                DeploymentStep step = STEPS.get(i);

                Map<String, Object> startPayload = new LinkedHashMap<>();
                startPayload.put("stepIndex", i);
                startPayload.put("stepId",    step.getId());
                startPayload.put("label",     step.getLabel());
                send(runId, "step-start", startPayload);

                // Replace this stub with real work: topic creation, Flink submit, etc.
                performStep(configurationYaml, step.getId(), i);

                send(runId, "step-complete", Map.of("stepIndex", i));
            }
            send(runId, "deployment-complete", Map.of("success", true));

        } catch (StepException e) {
            send(runId, "step-failed", Map.of(
                "stepIndex", e.getStepIndex(),
                "error",     e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during deployment runId={}", runId, e);
            send(runId, "deployment-failed", Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        } finally {
            // Signal the drain loop to stop, then close the live emitter if present
            LinkedBlockingQueue<Object> buffer = eventBuffers.get(runId);
            if (buffer != null) {
                buffer.offer(QUEUE_DONE);
            }
            SseEmitter emitter = emitters.remove(runId);
            if (emitter != null) {
                emitter.complete();
            }
        }
    }

    /**
     * Stub step execution — simulates work with a small sleep.
     * Replace each case with real logic (Kafka admin calls, Flink REST API, etc.).
     * {@code configurationYaml} is the raw pipeline YAML passed in from the deploy request.
     */
    @SuppressWarnings("unused")
    private void performStep(String configurationYaml, String stepId, int stepIndex) {
        try {
            // Simulate variable step duration
            long durationMs = switch (stepId) {
                case "validate-config-eyal"   -> 500;
                case "prepare-resources" -> 1200;
                case "validate-mappings" -> 600;
                case "prepare-flink"     -> 1500;
                case "upload-artifacts"  -> 800;
                case "register-pipeline" -> 700;
                case "deploy-job"        -> 2000;

                case "health-checks"     -> 1000;
                default -> 500;
            };
            Thread.sleep(durationMs);
            if ("deploy-job".equals(stepId) && isEtlJob1(configurationYaml)) {
                throw new StepException(stepIndex, "Flink job deployment failed: micky mouse said cluster unavailable,micky mouse said cluster unavailable" +
                        "\nmicky mouse said cluster unavailable!!!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StepException(stepIndex, "Step interrupted: " + stepId, e);
        }
    }

    /**
     * Returns {@code true} when the YAML body belongs to the "ETL Job1" product type.
     * Checks for the literal line {@code productType: ETL Job1} (case-sensitive).
     */
    private boolean isEtlJob1(String configurationYaml) {
        if (configurationYaml == null) return false;
        for (String line : configurationYaml.lines().toList()) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("productType:")) {
                String value = trimmed.substring("productType:".length()).strip();
                return "ETL Job1".equals(value);
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // SSE send helpers
    // -----------------------------------------------------------------------

    /**
     * Routes an event either directly to a live emitter or into the buffer if
     * the browser has not yet connected.
     */
    private void send(String runId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(runId);
        if (emitter != null) {
            sendToEmitter(emitter, eventName, data);
        } else {
            // Emitter not yet registered — buffer the event
            LinkedBlockingQueue<Object> buffer = eventBuffers.get(runId);
            if (buffer != null) {
                buffer.offer(new SseEvent(eventName, data));
            }
        }
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        logger.info("in send to emitter:"+eventName);
        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON)
            );
        } catch (IOException e) {
            logger.warn("Failed to send SSE event '{}': {}", eventName, e.getMessage());
            emitters.values().remove(emitter);
        }
    }

    // -----------------------------------------------------------------------
    // Internal record to hold a buffered event
    // -----------------------------------------------------------------------

    private record SseEvent(String name, Object data) {}
}

