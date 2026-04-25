package com.example.service.simulator;

import com.example.model.KafkaSimulatorDtos.*;
import com.example.model.KafkaSimulatorTaskPlan;
import com.example.repository.KafkaSimulatorTaskPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing saved Kafka simulator task plans.
 * Handles validation, persistence, and business logic.
 */
@Service
public class KafkaSimulatorTaskPlanService {

    private static final Logger log = LoggerFactory.getLogger(KafkaSimulatorTaskPlanService.class);

    private static final Set<String> ALLOWED_BROKER_ENVS = Set.of("CAP", "PROD", "HOME", "OFFICE", "HOME1", "OFFICE1");
    private static final Set<String> ALLOWED_MESSAGE_FORMATS = Set.of("json", "csv", "xml", "protobuf", "plain");

    private final KafkaSimulatorTaskPlanRepository repository;

    public KafkaSimulatorTaskPlanService(KafkaSimulatorTaskPlanRepository repository) {
        this.repository = repository;
    }

    /**
     * Save or update a task plan for the user.
     * Acts as upsert: if plan with same id/name exists, update it; otherwise create new.
     */
    public TaskPlanResponse saveTaskPlan(String userId, SaveTaskPlanRequest request) {
        validateSaveRequest(request);

        // Trim all strings
        String planId = request.getId() != null ? request.getId().trim() : null;
        String planName = request.getName() != null ? request.getName().trim() : null;
        String brokerEnv = request.getBrokerEnv() != null ? request.getBrokerEnv().trim() : null;
        String topic = request.getTopic() != null ? request.getTopic().trim() : null;

        // If only name is provided and no id, generate an id from normalized name
        if (planId == null && planName != null) {
            planId = normalize(planName);
        }

        String normalizedName = planName != null ? normalize(planName) : null;

        // Check for conflict: if both id and name are provided, ensure they reference the same plan
        if (planId != null && planName != null) {
            Optional<KafkaSimulatorTaskPlan> byId = repository.findByUserIdAndPlanId(userId, planId);
            Optional<KafkaSimulatorTaskPlan> byName = repository.findByUserIdAndNormalizedName(userId, normalizedName);

            if (byId.isPresent() && byName.isPresent()) {
                if (!byId.get().getPlanId().equals(byName.get().getPlanId())) {
                    throw new IllegalArgumentException("Plan ID and name reference different plans");
                }
            }
        }

        // Validate rows
        validateRows(request.getRows());

        String now = Instant.now().toString();
        KafkaSimulatorTaskPlan existing = repository.findByUserIdAndPlanId(userId, planId).orElse(null);

        String createdAt = existing != null ? existing.getCreatedAt() : now;
        String updatedAt = now;

        KafkaSimulatorTaskPlan plan = new KafkaSimulatorTaskPlan(
            userId, planId, planName, normalizedName, brokerEnv, topic, request.getRows(),
            createdAt, updatedAt
        );

        repository.save(plan);
        log.info("Saved task plan [userId={}, planId={}, planName={}]", userId, planId, planName);

        return toResponse(plan);
    }

    /**
     * Resolve a single task plan by id and/or name.
     * If both are provided, they must reference the same plan.
     */
    public TaskPlanResponse resolveTaskPlan(String userId, String planId, String planName) {
        if (!StringUtils.hasText(planId) && !StringUtils.hasText(planName)) {
            throw new IllegalArgumentException("Provide either plan ID or plan name");
        }

        KafkaSimulatorTaskPlan plan = null;

        if (StringUtils.hasText(planId)) {
            plan = repository.findByUserIdAndPlanId(userId, planId.trim()).orElse(null);
        }

        if (StringUtils.hasText(planName)) {
            String normalizedName = normalize(planName);
            Optional<KafkaSimulatorTaskPlan> byName = repository.findByUserIdAndNormalizedName(userId, normalizedName);

            if (byName.isPresent()) {
                if (plan != null && !plan.getPlanId().equals(byName.get().getPlanId())) {
                    throw new IllegalArgumentException("Plan ID and name reference different plans");
                }
                plan = byName.get();
            }
        }

        if (plan == null) {
            throw new IllegalArgumentException("Task plan not found");
        }

        log.info("Resolved task plan [userId={}, planId={}, planName={}]", userId, plan.getPlanId(), plan.getPlanName());
        return toResponse(plan);
    }

    /**
     * List all task plans for the user, sorted by most recently updated first.
     */
    public ListTaskPlansResponse listTaskPlans(String userId) {
        List<KafkaSimulatorTaskPlan> plans = repository.findAllByUserId(userId);
        List<TaskPlanResponse> responses = plans.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        log.info("Listed task plans [userId={}, count={}]", userId, responses.size());
        return new ListTaskPlansResponse(responses);
    }

    /**
     * Delete a task plan by id.
     */
//    public void deleteTaskPlan(String userId, String planId,String planName) {
//        repository.findByUserIdAndPlanId(userId, planId)
//            .orElseThrow(() -> new IllegalArgumentException("Task plan not found"));
//        repository.delete(userId, planId, planName);
//        log.info("Deleted task plan [userId={}, planId={}]", userId, planId);
//    }

    /**
     * Delete a task plan by id and/or name.
     */
    public void deleteTaskPlan(String userId, String planId, String planName) {
        if (!StringUtils.hasText(planId) && !StringUtils.hasText(planName)) {
            throw new IllegalArgumentException("Provide either plan ID or plan name");
        }

        KafkaSimulatorTaskPlan byId = null;
        if (StringUtils.hasText(planId)) {
            byId = repository.findByUserIdAndPlanId(userId, planId.trim()).orElse(null);
            if (byId == null && !StringUtils.hasText(planName)) {
                throw new IllegalArgumentException("Task plan not found");
            }
        }

        KafkaSimulatorTaskPlan target = byId;
        if (StringUtils.hasText(planName)) {
            String normalizedName = normalize(planName);
            KafkaSimulatorTaskPlan byName = repository.findByUserIdAndNormalizedName(userId, normalizedName).orElse(null);
            if (byName == null && byId == null) {
                throw new IllegalArgumentException("Task plan not found");
            }
            if (byId != null && byName != null && !byId.getPlanId().equals(byName.getPlanId())) {
                throw new IllegalArgumentException("Plan ID and name reference different plans");
            }
            if (target == null) {
                target = byName;
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("Task plan not found");
        }

        repository.delete(target.getUserId(), target.getPlanId(),target.getPlanName());
        log.info("Deleted task plan [userId={}, planId={}, planName={}]", userId, target.getPlanId(), target.getPlanName());
    }

    // ========== Validation ==========

    private void validateSaveRequest(SaveTaskPlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body required");
        }

        String id = request.getId();
        String name = request.getName();

        if (!StringUtils.hasText(id) && !StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Provide either plan ID or plan name");
        }

        String brokerEnv = request.getBrokerEnv();
        if (!StringUtils.hasText(brokerEnv)) {
            throw new IllegalArgumentException("Broker environment (brokerEnv) is required");
        }

        if (!ALLOWED_BROKER_ENVS.contains(brokerEnv.trim().toUpperCase())) {
            throw new IllegalArgumentException("Unknown broker environment: " + brokerEnv);
        }

        String topic = request.getTopic();
        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("Topic is required");
        }

        if (request.getRows() == null) {
            throw new IllegalArgumentException("Rows array is required");
        }
    }

    private void validateRows(List<TaskPlanRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("At least one row is required");
        }

        for (int i = 0; i < rows.size(); i++) {
            TaskPlanRow row = rows.get(i);
            validateRow(row, i);
        }
    }

    private void validateRow(TaskPlanRow row, int index) {
        if (row == null) {
            throw new IllegalArgumentException("Row " + index + " is null");
        }

        String rowId = row.getId();
        if (!StringUtils.hasText(rowId)) {
            throw new IllegalArgumentException("Row " + index + " must have an id");
        }

        String messageFormat = row.getMessageFormat();
        if (!StringUtils.hasText(messageFormat)) {
            throw new IllegalArgumentException("Row " + index + " messageFormat is required");
        }

        if (!ALLOWED_MESSAGE_FORMATS.contains(messageFormat.toLowerCase())) {
            throw new IllegalArgumentException("Row " + index + " has invalid messageFormat: " + messageFormat);
        }

        String sampleMessage = row.getSampleMessage();
        if (!StringUtils.hasText(sampleMessage)) {
            throw new IllegalArgumentException("Row " + index + " sampleMessage is required");
        }

        Integer mps = row.getMessagesPerSecond();
        if (mps == null || mps < 1 || mps > 10000) {
            throw new IllegalArgumentException("Row " + index + " messagesPerSecond must be 1-10000");
        }

        Integer total = row.getTotalMessages();
        if (total == null || (total < 1 && total != -1)) {
            throw new IllegalArgumentException("Row " + index + " totalMessages must be >= 1 or -1 (unlimited)");
        }

        Integer interval = row.getIntervalSeconds();
        if (interval == null || interval < 0) {
            throw new IllegalArgumentException("Row " + index + " intervalSeconds must be >= 0");
        }
    }

    // ========== Helpers ==========

    private String normalize(String text) {
        if (text == null) return null;
        return text.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private TaskPlanResponse toResponse(KafkaSimulatorTaskPlan plan) {
        return new TaskPlanResponse(
            plan.getPlanId(),
            plan.getPlanName(),
            plan.getBrokerEnv(),
            plan.getTopic(),
            plan.getRows(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }
}

