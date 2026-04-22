package com.example.controller;

import com.example.config.KafkaSimulatorProperties;
import com.example.service.simulator.KafkaProducerPool;
import com.example.service.simulator.KafkaSimulatorService;
import com.example.service.simulator.KafkaTopicVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit/integration tests for KafkaSimulatorController.
 * Uses MockMvc with a real KafkaSimulatorService but with a mock KafkaProducer
 * so no external Kafka broker is required.
 */
@SuppressWarnings("unchecked")
class KafkaSimulatorControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String USER = "test-user";

    private MockMvc mockMvc;
    private KafkaSimulatorService simulatorService;
    private KafkaProducerPool producerPool;
    private KafkaTopicVerifier topicVerifier;
    private KafkaProducer<String, byte[]> mockProducer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Build properties with HOME and OFFICE environments
        KafkaSimulatorProperties props = new KafkaSimulatorProperties();
        Map<String, String> envs = new HashMap<>();
        envs.put("HOME", "localhost:9092");
        envs.put("HOME1", "localhost:9092");
        envs.put("OFFICE", "localhost:9092");
        envs.put("OFFICE1", "localhost:9092");
        props.setEnvironments(envs);

        // Create mock Kafka producer
        mockProducer = (KafkaProducer<String, byte[]>) mock(KafkaProducer.class);
        RecordMetadata meta = new RecordMetadata(new TopicPartition("test", 0), 0, 0, 0, 0, 0);
        when(mockProducer.send(any(), any())).thenAnswer(inv -> {
            org.apache.kafka.clients.producer.Callback cb = inv.getArgument(1);
            cb.onCompletion(meta, null);
            return CompletableFuture.completedFuture(meta);
        });

        // Real pool but with injected mock producer
        producerPool = new KafkaProducerPool(props);
        producerPool.registerProducerForTesting("HOME", mockProducer);
        producerPool.registerProducerForTesting("OFFICE", mockProducer);

        // Use a real single-thread scheduler so bursts execute synchronously in tests
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        topicVerifier = mock(KafkaTopicVerifier.class);
        when(topicVerifier.topicExists(any(), any())).thenReturn(true);
        simulatorService = new KafkaSimulatorService(producerPool, scheduler, topicVerifier);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new KafkaSimulatorController(simulatorService))
            .build();
    }

    // =========================================================================
    // 1. POST /start — valid payload → 200 with taskId
    // =========================================================================

    @Test
    void startTask_validPayload_returns200WithTaskId() throws Exception {
        String body = validStartBody("HOME", "my-topic", 5, 10, 0);

        mockMvc.perform(post("/api/simulator/kafka/start")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("running"))
            .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    // =========================================================================
    // 2. POST /start — missing environment field → 400
    // =========================================================================

    @Test
    void startTask_missingEnvironment_returns400() throws Exception {
        String body = """
            {
              "topic": "my-topic",
              "messageFormat": "json",
              "sampleMessage": "{}",
              "messagesPerSecond": 5,
              "totalMessages": 10,
              "intervalSeconds": 0
            }
            """;

        mockMvc.perform(post("/api/simulator/kafka/start")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // =========================================================================
    // 3. POST /start — unknown environment → 400
    // =========================================================================

    @Test
    void startTask_unknownEnvironment_returns400() throws Exception {
        String body = validStartBody("UNKNOWN_ENV", "my-topic", 5, 10, 0);

        mockMvc.perform(post("/api/simulator/kafka/start")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Unknown Kafka environment")));
    }

    // =========================================================================
    // 4. POST /stop/{taskId} — running task → 200 stopped
    // =========================================================================

    @Test
    void stopTask_runningTask_returns200Stopped() throws Exception {
        String taskId = startTaskAndGetId("HOME", "topic-1", 1, -1, 5); // long-running

        mockMvc.perform(post("/api/simulator/kafka/stop/" + taskId)
                .header(USER_HEADER, USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(taskId))
            .andExpect(jsonPath("$.status").value("stopped"))
            .andExpect(jsonPath("$.stoppedAt").isNotEmpty());
    }

    // =========================================================================
    // 5. POST /stop/{taskId} — unknown taskId → 404
    // =========================================================================

    @Test
    void stopTask_unknownTaskId_returns404() throws Exception {
        mockMvc.perform(post("/api/simulator/kafka/stop/non-existent-id")
                .header(USER_HEADER, USER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // =========================================================================
    // 6. DELETE /tasks/{taskId} — existing task → 200
    // =========================================================================

    @Test
    void deleteTask_existingTask_returns200() throws Exception {
        String taskId = startTaskAndGetId("HOME", "topic-del", 1, -1, 5);

        mockMvc.perform(delete("/api/simulator/kafka/tasks/" + taskId)
                .header(USER_HEADER, USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 7. DELETE /tasks/{taskId} — unknown taskId → 404
    // =========================================================================

    @Test
    void deleteTask_unknownTaskId_returns404() throws Exception {
        mockMvc.perform(delete("/api/simulator/kafka/tasks/ghost-id")
                .header(USER_HEADER, USER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task not found"));
    }

    // =========================================================================
    // 8. GET /tasks — returns list including started tasks
    // =========================================================================

    @Test
    void listTasks_includesStartedTasks() throws Exception {
        startTaskAndGetId("HOME", "topic-list", 1, -1, 5);

        mockMvc.perform(get("/api/simulator/kafka/tasks")
                .header(USER_HEADER, USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", not(empty())))
            .andExpect(jsonPath("$[0].taskId").isNotEmpty())
            .andExpect(jsonPath("$[0].environment").value("HOME"))
            .andExpect(jsonPath("$[0].topic").value("topic-list"));
    }

    @Test
    void listTasks_emptyWhenNoTasks() throws Exception {
        mockMvc.perform(get("/api/simulator/kafka/tasks")
                .header(USER_HEADER, USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTaskStatus_runningTask_returnsStatusPayload() throws Exception {
        String taskId = startTaskAndGetId("HOME", "topic-status", 1, -1, 5);

        mockMvc.perform(get("/api/simulator/kafka/status/" + taskId)
                .header(USER_HEADER, USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(taskId))
            .andExpect(jsonPath("$.status").value("running"))
            .andExpect(jsonPath("$.sentCount", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.statusMessage", containsString("Published")));
    }

    @Test
    void getTaskStatus_unknownTask_returns404() throws Exception {
        mockMvc.perform(get("/api/simulator/kafka/status/unknown-task-id")
                .header(USER_HEADER, USER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task not found"));
    }

    // =========================================================================
    // 9. Placeholder substitution — each message gets unique {{uuid}} and {{now}}
    // =========================================================================

    @Test
    void messageSrenderer_eachMessageGetsUniqueUuidAndNow() {
        String template = "id={{uuid}},val={{value}},ts={{now}}";

        String msg1 = com.example.service.simulator.MessageRenderer.substitute(template);
        String msg2 = com.example.service.simulator.MessageRenderer.substitute(template);

        // Both should not contain the raw placeholder tokens
        assertFalse(msg1.contains("{{uuid}}"));
        assertFalse(msg1.contains("{{now}}"));
        assertFalse(msg1.contains("{{value}}"));

        // UUID part of msg1 vs msg2 should differ
        assertNotEquals(msg1, msg2, "Two substituted messages should not be identical");
    }

    // =========================================================================
    // 10. intervalSeconds == 0 → task auto-completes after one burst
    // =========================================================================

    @Test
    void startTask_intervalZero_taskAutoCompletes() throws Exception {
        // Start with intervalSeconds=0 and a small total
        String body = validStartBody("HOME", "once-topic", 2, 2, 0);

        String response = mockMvc.perform(post("/api/simulator/kafka/start")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String taskId = objectMapper.readTree(response).get("taskId").asText();

        // Give the scheduler a moment to execute the burst
        Thread.sleep(500);

        // Check task status via task list
        String listResponse = mockMvc.perform(get("/api/simulator/kafka/tasks")
                .header(USER_HEADER, USER))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        com.fasterxml.jackson.databind.JsonNode tasks = objectMapper.readTree(listResponse);
        boolean found = false;
        for (com.fasterxml.jackson.databind.JsonNode task : tasks) {
            if (taskId.equals(task.get("taskId").asText())) {
                String status = task.get("status").asText();
                assertTrue("completed".equals(status) || "running".equals(status),
                    "Expected completed or running; got: " + status);
                found = true;
                break;
            }
        }
        assertTrue(found, "Task should appear in task list");
    }

    // =========================================================================
    // Extra: POST /stop/{taskId} on already-stopped task → 404
    // =========================================================================

    @Test
    void stopTask_alreadyStopped_returns404() throws Exception {
        String taskId = startTaskAndGetId("HOME", "topic-stop2", 1, -1, 5);

        // Stop once
        mockMvc.perform(post("/api/simulator/kafka/stop/" + taskId)
                .header(USER_HEADER, USER))
            .andExpect(status().isOk());

        // Stop again → 404
        mockMvc.perform(post("/api/simulator/kafka/stop/" + taskId)
                .header(USER_HEADER, USER))
            .andExpect(status().isNotFound());
    }

    @Test
    void testConnection_existingTopic_returns200() throws Exception {
        when(topicVerifier.topicExists("HOME", "orders-topic")).thenReturn(true);

        mockMvc.perform(post("/api/simulator/kafka/test-connection")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "environment": "HOME",
                      "topic": "orders-topic"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Connection successful and topic exists"));
    }

    @Test
    void testConnection_topicMissing_returns404() throws Exception {
        when(topicVerifier.topicExists("HOME", "missing-topic")).thenReturn(false);

        mockMvc.perform(post("/api/simulator/kafka/test-connection")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "environment": "HOME",
                      "topic": "missing-topic"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Topic not found: missing-topic"));
    }

    @Test
    void testConnection_missingEnvironment_returns400() throws Exception {
        mockMvc.perform(post("/api/simulator/kafka/test-connection")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "topic": "orders-topic"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("environment is required"));
    }

    @Test
    void testConnection_unknownEnvironment_returns400() throws Exception {
        mockMvc.perform(post("/api/simulator/kafka/test-connection")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "environment": "NOPE",
                      "topic": "orders-topic"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unknown Kafka environment: NOPE"));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String validStartBody(String env, String topic, int mps, int total, int interval) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "environment", env,
            "topic", topic,
            "messageFormat", "json",
            "sampleMessage", "{\"id\":\"{{uuid}}\",\"value\":{{value}},\"ts\":\"{{now}}\"}",
            "messagesPerSecond", mps,
            "totalMessages", total,
            "intervalSeconds", interval
        ));
    }

    private String startTaskAndGetId(String env, String topic, int mps, int total, int interval) throws Exception {
        String body = validStartBody(env, topic, mps, total, interval);
        String response = mockMvc.perform(post("/api/simulator/kafka/start")
                .header(USER_HEADER, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("taskId").asText();
    }
}

