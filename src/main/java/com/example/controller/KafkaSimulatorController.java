package com.example.controller;

import com.example.model.KafkaSimulatorDtos.*;
import com.example.service.simulator.KafkaSimulatorService;
import com.example.service.simulator.KafkaSimulatorTask;
import com.example.service.simulator.TopicNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulator/kafka")
@Tag(name = "Kafka Simulator", description = "Publish synthetic test messages to Kafka topics at controlled rates")
public class KafkaSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(KafkaSimulatorController.class);

    private final KafkaSimulatorService simulatorService;
    private final com.example.service.simulator.KafkaSimulatorTaskPlanService taskPlanService;

    public KafkaSimulatorController(KafkaSimulatorService simulatorService,
                                    com.example.service.simulator.KafkaSimulatorTaskPlanService taskPlanService) {
        this.simulatorService = simulatorService;
        this.taskPlanService = taskPlanService;
    }

    // -------------------------------------------------------------------------
    // POST /simulator/kafka/start
    // -------------------------------------------------------------------------

    @PostMapping(value = "/start",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Start a new Kafka simulation task")
    public ResponseEntity<?> startTask(
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @RequestBody StartTaskRequest request) {

        log.info("POST /api/simulator/kafka/start [user={}, env={}, topic={}]",
            userId, request.getEnvironment(), request.getTopic());
        try {
            KafkaSimulatorTask task = simulatorService.startTask(request);
            StartTaskResponse response = new StartTaskResponse(task.getTaskId(), "running", task.getStartedAt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to start simulation task", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to start task: " + ex.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /simulator/kafka/stop/{taskId}
    // -------------------------------------------------------------------------

    @PostMapping(value = "/stop/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Stop a running Kafka simulation task")
    public ResponseEntity<?> stopTask(
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @PathVariable String taskId) {

        log.info("POST /api/simulator/kafka/stop/{} [user={}]", taskId, userId);
        StopTaskResponse response = simulatorService.stopTask(taskId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Task not found or already stopped"));
        }
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // DELETE /simulator/kafka/tasks/{taskId}
    // -------------------------------------------------------------------------

    @DeleteMapping(value = "/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a simulation task record (stops it if still running)")
    public ResponseEntity<?> deleteTask(
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @PathVariable String taskId) {

        log.info("DELETE /api/simulator/kafka/tasks/{} [user={}]", taskId, userId);
        boolean deleted = simulatorService.deleteTask(taskId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Task not found"));
        }
        return ResponseEntity.ok(new DeleteResponse(true));
    }

    // -------------------------------------------------------------------------
    // GET /simulator/kafka/tasks
    // -------------------------------------------------------------------------

    @GetMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all simulation tasks (active + recent)")
    public ResponseEntity<List<TaskSummary>> listTasks(
        @RequestHeader(value = "X-User-Id", required = false) String userId) {

        log.info("GET /api/simulator/kafka/tasks [user={}]", userId);
        return ResponseEntity.ok(simulatorService.listTasks());
    }

    @GetMapping(value = "/status/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get status for a single simulation task")
    public ResponseEntity<?> getTaskStatus(
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @PathVariable String taskId) {

        log.info("GET /api/simulator/kafka/status/{} [user={}]", taskId, userId);
        try {
            return ResponseEntity.ok(simulatorService.getTaskStatus(taskId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Task not found"));
        }
    }

    @PostMapping(value = "/test-connection",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify Kafka connectivity and topic existence")
    public ResponseEntity<?> testConnection(
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @RequestBody TestConnectionRequest request) {

        log.info("POST /api/simulator/kafka/test-connection [user={}, env={}, topic={}]",
            userId, request != null ? request.getEnvironment() : null, request != null ? request.getTopic() : null);
        try {
            return ResponseEntity.ok(simulatorService.testConnection(request));
        } catch (TopicNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to verify Kafka connection", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ex.getMessage()));
        }
    }

    // =========================================================================
    // Task Plan Endpoints
    // =========================================================================

    @GetMapping(value = "/task-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all saved task plans for the current user")
    public ResponseEntity<?> listTaskPlans(
        @RequestHeader(value = "X-User-Id") String userId) {

        log.info("GET /api/simulator/kafka/task-plans [user={}]", userId);
        try {
            return ResponseEntity.ok(taskPlanService.listTaskPlans(userId));
        } catch (Exception ex) {
            log.error("Failed to list task plans", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to list task plans: " + ex.getMessage()));
        }
    }

    @PostMapping(value = "/task-plans",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Save or update a task plan")
    public ResponseEntity<?> saveTaskPlan(
        @RequestHeader(value = "X-User-Id") String userId,
        @RequestBody SaveTaskPlanRequest request) {

        log.info("POST /api/simulator/kafka/task-plans [user={}, planId={}, planName={}]",
            userId, request != null ? request.getId() : null, request != null ? request.getName() : null);
        try {
            TaskPlanResponse response = taskPlanService.saveTaskPlan(userId, request);
            return ResponseEntity.ok(new SaveTaskPlanResponse(response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to save task plan", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to save task plan: " + ex.getMessage()));
        }
    }

    @GetMapping(value = "/task-plans/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resolve and load a single task plan by id and/or name")
    public ResponseEntity<?> resolveTaskPlan(
        @RequestHeader(value = "X-User-Id") String userId,
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String name) {

        log.info("GET /api/simulator/kafka/task-plans/resolve [user={}, id={}, name={}]", userId, id, name);
        try {
            TaskPlanResponse response = taskPlanService.resolveTaskPlan(userId, id, name);
            return ResponseEntity.ok(new ResolveTaskPlanResponse(response));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to resolve task plan", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to resolve task plan: " + ex.getMessage()));
        }
    }

    @DeleteMapping(value = "/task-plans/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a saved task plan by id and/or name")
    public ResponseEntity<?> deleteTaskPlan(
        @RequestHeader(value = "X-User-Id") String userId,
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String name) {

        log.info("DELETE /api/simulator/kafka/task-plans [user={}, id={}, name={}]", userId, id, name);
        try {
            taskPlanService.deleteTaskPlan(userId, id, name);
            return ResponseEntity.ok(new DeleteResponse(true));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to delete task plan", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to delete task plan: " + ex.getMessage()));
        }
    }
}
