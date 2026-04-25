package com.example.repository;

import com.example.model.KafkaSimulatorTaskPlan;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for task plans, scoped by user.
 * Thread-safe using ConcurrentHashMap.
 */
@Repository
public class KafkaSimulatorTaskPlanRepository {

    // Composite key: "userId::planId::normalizedPlanName"
    private final Map<String, KafkaSimulatorTaskPlan> store = new ConcurrentHashMap<>();

    public void save(KafkaSimulatorTaskPlan plan) {
        String key = buildKey(plan.getUserId(), plan.getPlanId(), plan.getNormalizedPlanName());
        store.put(key, plan);
    }

    public Optional<KafkaSimulatorTaskPlan> findByUserIdAndPlanId(String userId, String planId) {
        return store.values().stream()
            .filter(p -> p.getUserId().equals(userId) && planId.equals(p.getPlanId()))
            .findFirst();
    }

    public Optional<KafkaSimulatorTaskPlan> findByUserIdAndNormalizedName(String userId, String normalizedName) {
        return store.values().stream()
            .filter(p -> p.getUserId().equals(userId) && normalizedName.equals(p.getNormalizedPlanName()))
            .findFirst();
    }

    public List<KafkaSimulatorTaskPlan> findAllByUserId(String userId) {
        return store.values().stream()
            .filter(p -> p.getUserId().equals(userId))
            .sorted(Comparator.comparing(KafkaSimulatorTaskPlan::getUpdatedAt).reversed())
            .collect(Collectors.toList());
    }

    public void delete(String userId, String planId, String planName) {
        store.values().removeIf(p -> p.getUserId().equals(userId) && planId.equals(p.getPlanId()) && p.getPlanName().equals(planName));
    }

    public void deleteByUserIdAndName(String userId, String normalizedName) {
        store.values().removeIf(p -> p.getUserId().equals(userId) && normalizedName.equals(p.getNormalizedPlanName()));
    }

    private String buildKey(String userId, String planId, String normalizedName) {
        return userId + "::" + planId + "::" + normalizedName;
    }

    // For testing/debugging
    public void clear() {
        store.clear();
    }
}

