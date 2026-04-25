package com.example.model;

import java.util.List;
import java.util.Objects;

/**
 * Internal model for persisted task plans scoped by user.
 * Not exposed directly in responses; mapped to/from DTOs.
 */
public class KafkaSimulatorTaskPlan {
    private String userId;
    private String planId;
    private String planName;
    private String normalizedPlanName;
    private String brokerEnv;
    private String topic;
    private List<KafkaSimulatorDtos.TaskPlanRow> rows;
    private String createdAt;
    private String updatedAt;

    public KafkaSimulatorTaskPlan() {}

    public KafkaSimulatorTaskPlan(String userId, String planId, String planName,
                                 String normalizedPlanName, String brokerEnv, String topic,
                                 List<KafkaSimulatorDtos.TaskPlanRow> rows, String createdAt, String updatedAt) {
        this.userId = userId;
        this.planId = planId;
        this.planName = planName;
        this.normalizedPlanName = normalizedPlanName;
        this.brokerEnv = brokerEnv;
        this.topic = topic;
        this.rows = rows;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getNormalizedPlanName() { return normalizedPlanName; }
    public void setNormalizedPlanName(String normalizedPlanName) { this.normalizedPlanName = normalizedPlanName; }

    public String getBrokerEnv() { return brokerEnv; }
    public void setBrokerEnv(String brokerEnv) { this.brokerEnv = brokerEnv; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public List<KafkaSimulatorDtos.TaskPlanRow> getRows() { return rows; }
    public void setRows(List<KafkaSimulatorDtos.TaskPlanRow> rows) { this.rows = rows; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaSimulatorTaskPlan plan = (KafkaSimulatorTaskPlan) o;
        return Objects.equals(userId, plan.userId) &&
               Objects.equals(planId, plan.planId) &&
               Objects.equals(normalizedPlanName, plan.normalizedPlanName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, planId, normalizedPlanName);
    }
}

