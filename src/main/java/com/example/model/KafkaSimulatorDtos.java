package com.example.model;

public class KafkaSimulatorDtos {

    public static class TestConnectionRequest {
        private String environment;
        private String topic;

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
    }

    public static class TestConnectionResponse {
        private boolean success;
        private String message;

        public TestConnectionResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class TaskStatusResponse {
        private String taskId;
        private String status;
        private long sentCount;
        private String statusMessage;

        public TaskStatusResponse(String taskId, String status, long sentCount, String statusMessage) {
            this.taskId = taskId;
            this.status = status;
            this.sentCount = sentCount;
            this.statusMessage = statusMessage;
        }

        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public long getSentCount() { return sentCount; }
        public String getStatusMessage() { return statusMessage; }
    }

    public static class StartTaskRequest {
        private String environment;
        private String topic;
        private String messageFormat;
        private String sampleMessage;
        private Integer messagesPerSecond;
        private Integer totalMessages;
        private Integer intervalSeconds;

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public String getMessageFormat() { return messageFormat; }
        public void setMessageFormat(String messageFormat) { this.messageFormat = messageFormat; }

        public String getSampleMessage() { return sampleMessage; }
        public void setSampleMessage(String sampleMessage) { this.sampleMessage = sampleMessage; }

        public Integer getMessagesPerSecond() { return messagesPerSecond; }
        public void setMessagesPerSecond(Integer messagesPerSecond) { this.messagesPerSecond = messagesPerSecond; }

        public Integer getTotalMessages() { return totalMessages; }
        public void setTotalMessages(Integer totalMessages) { this.totalMessages = totalMessages; }

        public Integer getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    }

    public static class StartTaskResponse {
        private String taskId;
        private String status;
        private String startedAt;

        public StartTaskResponse(String taskId, String status, String startedAt) {
            this.taskId = taskId;
            this.status = status;
            this.startedAt = startedAt;
        }

        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public String getStartedAt() { return startedAt; }
    }

    public static class StopTaskResponse {
        private String taskId;
        private String status;
        private String stoppedAt;

        public StopTaskResponse(String taskId, String status, String stoppedAt) {
            this.taskId = taskId;
            this.status = status;
            this.stoppedAt = stoppedAt;
        }

        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public String getStoppedAt() { return stoppedAt; }
    }

    public static class TaskSummary {
        private String taskId;
        private String environment;
        private String topic;
        private String messageFormat;
        private int messagesPerSecond;
        private int totalMessages;
        private int intervalSeconds;
        private String status;
        private long sentCount;
        private String startedAt;
        private String stoppedAt;

        public TaskSummary(String taskId, String environment, String topic, String messageFormat,
                           int messagesPerSecond, int totalMessages, int intervalSeconds,
                           String status, long sentCount, String startedAt, String stoppedAt) {
            this.taskId = taskId;
            this.environment = environment;
            this.topic = topic;
            this.messageFormat = messageFormat;
            this.messagesPerSecond = messagesPerSecond;
            this.totalMessages = totalMessages;
            this.intervalSeconds = intervalSeconds;
            this.status = status;
            this.sentCount = sentCount;
            this.startedAt = startedAt;
            this.stoppedAt = stoppedAt;
        }

        public String getTaskId() { return taskId; }
        public String getEnvironment() { return environment; }
        public String getTopic() { return topic; }
        public String getMessageFormat() { return messageFormat; }
        public int getMessagesPerSecond() { return messagesPerSecond; }
        public int getTotalMessages() { return totalMessages; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public String getStatus() { return status; }
        public long getSentCount() { return sentCount; }
        public String getStartedAt() { return startedAt; }
        public String getStoppedAt() { return stoppedAt; }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
    }

    public static class DeleteResponse {
        private boolean success;

        public DeleteResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() { return success; }
    }

    // ========== Task Plan DTOs ==========

    public static class TaskPlanRow {
        private String id;
        private String messageFormat;
        private String sampleMessage;
        private Integer messagesPerSecond;
        private Integer totalMessages;
        private Integer intervalSeconds;

        public TaskPlanRow() {}

        public TaskPlanRow(String id, String messageFormat, String sampleMessage,
                          Integer messagesPerSecond, Integer totalMessages, Integer intervalSeconds) {
            this.id = id;
            this.messageFormat = messageFormat;
            this.sampleMessage = sampleMessage;
            this.messagesPerSecond = messagesPerSecond;
            this.totalMessages = totalMessages;
            this.intervalSeconds = intervalSeconds;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getMessageFormat() { return messageFormat; }
        public void setMessageFormat(String messageFormat) { this.messageFormat = messageFormat; }

        public String getSampleMessage() { return sampleMessage; }
        public void setSampleMessage(String sampleMessage) { this.sampleMessage = sampleMessage; }

        public Integer getMessagesPerSecond() { return messagesPerSecond; }
        public void setMessagesPerSecond(Integer messagesPerSecond) { this.messagesPerSecond = messagesPerSecond; }

        public Integer getTotalMessages() { return totalMessages; }
        public void setTotalMessages(Integer totalMessages) { this.totalMessages = totalMessages; }

        public Integer getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    }

    public static class SaveTaskPlanRequest {
        private String id;
        private String name;
        private String brokerEnv;
        private String topic;
        private java.util.List<TaskPlanRow> rows;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBrokerEnv() { return brokerEnv; }
        public void setBrokerEnv(String brokerEnv) { this.brokerEnv = brokerEnv; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public java.util.List<TaskPlanRow> getRows() { return rows; }
        public void setRows(java.util.List<TaskPlanRow> rows) { this.rows = rows; }
    }

    public static class TaskPlanResponse {
        private String id;
        private String name;
        private String brokerEnv;
        private String topic;
        private java.util.List<TaskPlanRow> rows;
        private String createdAt;
        private String updatedAt;

        public TaskPlanResponse() {}

        public TaskPlanResponse(String id, String name, String brokerEnv, String topic,
                               java.util.List<TaskPlanRow> rows, String createdAt, String updatedAt) {
            this.id = id;
            this.name = name;
            this.brokerEnv = brokerEnv;
            this.topic = topic;
            this.rows = rows;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBrokerEnv() { return brokerEnv; }
        public void setBrokerEnv(String brokerEnv) { this.brokerEnv = brokerEnv; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public java.util.List<TaskPlanRow> getRows() { return rows; }
        public void setRows(java.util.List<TaskPlanRow> rows) { this.rows = rows; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ListTaskPlansResponse {
        private java.util.List<TaskPlanResponse> plans;

        public ListTaskPlansResponse() {}

        public ListTaskPlansResponse(java.util.List<TaskPlanResponse> plans) {
            this.plans = plans;
        }

        public java.util.List<TaskPlanResponse> getPlans() { return plans; }
        public void setPlans(java.util.List<TaskPlanResponse> plans) { this.plans = plans; }
    }

    public static class SaveTaskPlanResponse {
        private TaskPlanResponse plan;

        public SaveTaskPlanResponse() {}

        public SaveTaskPlanResponse(TaskPlanResponse plan) {
            this.plan = plan;
        }

        public TaskPlanResponse getPlan() { return plan; }
        public void setPlan(TaskPlanResponse plan) { this.plan = plan; }
    }

    public static class ResolveTaskPlanResponse {
        private TaskPlanResponse plan;

        public ResolveTaskPlanResponse() {}

        public ResolveTaskPlanResponse(TaskPlanResponse plan) {
            this.plan = plan;
        }

        public TaskPlanResponse getPlan() { return plan; }
        public void setPlan(TaskPlanResponse plan) { this.plan = plan; }
    }
}
