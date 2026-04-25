package com.example.service.simulator;

import com.example.model.KafkaSimulatorDtos.*;
import com.example.repository.KafkaSimulatorTaskPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KafkaSimulatorTaskPlanService.
 */
public class KafkaSimulatorTaskPlanServiceTest {

    private KafkaSimulatorTaskPlanRepository repository;
    private KafkaSimulatorTaskPlanService service;

    @BeforeEach
    void setUp() {
        repository = new KafkaSimulatorTaskPlanRepository();
        service = new KafkaSimulatorTaskPlanService(repository);
    }

    @Test
    void testSaveNewPlan() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Nightly Smoke");
        TaskPlanResponse response = service.saveTaskPlan("user123", request);

        assertNotNull(response);
        assertEquals("plan-1", response.getId());
        assertEquals("Nightly Smoke", response.getName());
        assertEquals("CAP", response.getBrokerEnv());
        assertEquals("sim-topic", response.getTopic());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
        assertEquals(response.getCreatedAt(), response.getUpdatedAt());
    }

    @Test
    void testUpdateExistingPlan() {
        // Save initial
        SaveTaskPlanRequest req1 = createValidRequest("plan-1", "Original");
        TaskPlanResponse resp1 = service.saveTaskPlan("user123", req1);
        String createdAt = resp1.getCreatedAt();

        // Wait a tiny bit to ensure updatedAt is different
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        // Update
        SaveTaskPlanRequest req2 = createValidRequest("plan-1", "Updated");
        req2.setTopic("new-topic");
        TaskPlanResponse resp2 = service.saveTaskPlan("user123", req2);

        assertEquals(createdAt, resp2.getCreatedAt());
        assertNotEquals(resp1.getUpdatedAt(), resp2.getUpdatedAt());
        assertEquals("Updated", resp2.getName());
        assertEquals("new-topic", resp2.getTopic());
    }

    @Test
    void testListPlansForUser() {
        service.saveTaskPlan("user123", createValidRequest("plan-1", "Plan 1"));
        service.saveTaskPlan("user123", createValidRequest("plan-2", "Plan 2"));
        service.saveTaskPlan("user456", createValidRequest("plan-3", "Plan 3")); // Different user

        ListTaskPlansResponse response = service.listTaskPlans("user123");
        assertEquals(2, response.getPlans().size());
        java.util.Set<String> ids = response.getPlans().stream()
            .map(TaskPlanResponse::getId)
            .collect(java.util.stream.Collectors.toSet());
        assertTrue(ids.contains("plan-1"));
        assertTrue(ids.contains("plan-2"));
    }

    @Test
    void testResolveById() {
        service.saveTaskPlan("user123", createValidRequest("plan-1", "Test Plan"));

        TaskPlanResponse response = service.resolveTaskPlan("user123", "plan-1", null);
        assertNotNull(response);
        assertEquals("plan-1", response.getId());
        assertEquals("Test Plan", response.getName());
    }

    @Test
    void testResolveByName() {
        service.saveTaskPlan("user123", createValidRequest("plan-1", "Test Plan"));

        TaskPlanResponse response = service.resolveTaskPlan("user123", null, "Test Plan");
        assertNotNull(response);
        assertEquals("plan-1", response.getId());
        assertEquals("Test Plan", response.getName());
    }

    @Test
    void testResolveByIdAndName_SamePlan() {
        service.saveTaskPlan("user123", createValidRequest("plan-1", "Test Plan"));

        TaskPlanResponse response = service.resolveTaskPlan("user123", "plan-1", "Test Plan");
        assertNotNull(response);
        assertEquals("plan-1", response.getId());
        assertEquals("Test Plan", response.getName());
    }

    @Test
    void testResolveNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.resolveTaskPlan("user123", "non-existent", null);
        });
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testResolveNoIdNoName() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.resolveTaskPlan("user123", null, null);
        });
        assertTrue(ex.getMessage().contains("plan ID or plan name"));
    }

    @Test
    void testValidationMissingBrokerEnv() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        request.setBrokerEnv(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("Broker environment"));
    }

    @Test
    void testValidationUnknownBrokerEnv() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        request.setBrokerEnv("UNKNOWN_ENV");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("Unknown broker environment"));
    }

    @Test
    void testValidationMissingTopic() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        request.setTopic(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("Topic is required"));
    }

    @Test
    void testValidationMissingIdAndName() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        request.setId(null);
        request.setName(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("plan ID or plan name"));
    }

    @Test
    void testValidationMissingRows() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        request.setRows(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("Rows"));
    }

    @Test
    void testValidationInvalidMessageFormat() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        TaskPlanRow row = createValidRow();
        row.setMessageFormat("INVALID_FORMAT");
        request.setRows(Arrays.asList(row));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("messageFormat"));
    }

    @Test
    void testValidationMessagesPerSecondTooLow() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        TaskPlanRow row = createValidRow();
        row.setMessagesPerSecond(0);
        request.setRows(Arrays.asList(row));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("messagesPerSecond"));
    }

    @Test
    void testValidationMessagesPerSecondTooHigh() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        TaskPlanRow row = createValidRow();
        row.setMessagesPerSecond(10001);
        request.setRows(Arrays.asList(row));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("messagesPerSecond"));
    }

    @Test
    void testValidationTotalMessagesInvalid() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        TaskPlanRow row = createValidRow();
        row.setTotalMessages(0); // Invalid: must be >= 1 or -1
        request.setRows(Arrays.asList(row));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("totalMessages"));
    }

    @Test
    void testValidationIntervalSecondsNegative() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Test");
        TaskPlanRow row = createValidRow();
        row.setIntervalSeconds(-1);
        request.setRows(Arrays.asList(row));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.saveTaskPlan("user123", request);
        });
        assertTrue(ex.getMessage().contains("intervalSeconds"));
    }

    @Test
    void testNameNormalization() {
        // Plan saved with name, should generate id from normalized name
        SaveTaskPlanRequest request = new SaveTaskPlanRequest();
        request.setName("My Test Plan");
        request.setBrokerEnv("CAP");
        request.setTopic("topic");
        request.setRows(Arrays.asList(createValidRow()));

        TaskPlanResponse response = service.saveTaskPlan("user123", request);
        
        // Should have generated an id from normalized name
        assertNotNull(response.getId());
        assertEquals("my_test_plan", response.getId());
    }

    @Test
    void testUserScoping() {
        SaveTaskPlanRequest request = createValidRequest("plan-1", "Plan 1");
        service.saveTaskPlan("user123", request);

        // user456 should not see user123's plan
        ListTaskPlansResponse response = service.listTaskPlans("user456");
        assertEquals(0, response.getPlans().size());

        // user123 should see it
        response = service.listTaskPlans("user123");
        assertEquals(1, response.getPlans().size());
    }

    @Test
    void testDeleteById() {
        service.saveTaskPlan("user123", createValidRequest("plan-1", "Plan One"));

        service.deleteTaskPlan("user123", "plan-1", null);

        assertThrows(IllegalArgumentException.class, () ->
            service.resolveTaskPlan("user123", "plan-1", null));
    }

    @Test
    void testDeleteByName() {
        service.saveTaskPlan("user123", createValidRequest("plan-1", "Plan One"));

        service.deleteTaskPlan("user123", null, "Plan One");

        assertThrows(IllegalArgumentException.class, () ->
            service.resolveTaskPlan("user123", "plan-1", null));
    }

    @Test
    void testDeleteNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            service.deleteTaskPlan("user123", "missing", null));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testDeleteConflictWhenIdAndNameMismatch() {
        service.saveTaskPlan("user123", createValidRequest("plan-1", "Plan One"));
        service.saveTaskPlan("user123", createValidRequest("plan-2", "Plan Two"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            service.deleteTaskPlan("user123", "plan-1", "Plan Two"));
        assertTrue(ex.getMessage().contains("different plans"));
    }

    // ========== Helpers ==========

    private SaveTaskPlanRequest createValidRequest(String id, String name) {
        SaveTaskPlanRequest request = new SaveTaskPlanRequest();
        request.setId(id);
        request.setName(name);
        request.setBrokerEnv("CAP");
        request.setTopic("sim-topic");
        request.setRows(Arrays.asList(createValidRow()));
        return request;
    }

    private TaskPlanRow createValidRow() {
        return new TaskPlanRow("row-1", "json", "{\"test\":1}", 5, 50, 5);
    }
}
