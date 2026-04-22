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
}

