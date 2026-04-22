package com.example.service.simulator;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaSimulatorTask {

    public enum Status { RUNNING, STOPPED, COMPLETED, ERROR }

    private final String taskId;
    private final String environment;
    private final String topic;
    private final String messageFormat;
    private final String sampleMessage;
    private final int messagesPerSecond;
    private final int totalMessages;
    private final int intervalSeconds;
    private final String startedAt;

    private volatile Status status = Status.RUNNING;
    private volatile String stoppedAt;
    private volatile String lastError;
    private final AtomicLong sentCount = new AtomicLong(0);

    private volatile ScheduledFuture<?> scheduledFuture;

    public KafkaSimulatorTask(String taskId, String environment, String topic, String messageFormat,
                              String sampleMessage, int messagesPerSecond, int totalMessages,
                              int intervalSeconds, String startedAt) {
        this.taskId = taskId;
        this.environment = environment;
        this.topic = topic;
        this.messageFormat = messageFormat;
        this.sampleMessage = sampleMessage;
        this.messagesPerSecond = messagesPerSecond;
        this.totalMessages = totalMessages;
        this.intervalSeconds = intervalSeconds;
        this.startedAt = startedAt;
    }

    public String getTaskId() { return taskId; }
    public String getEnvironment() { return environment; }
    public String getTopic() { return topic; }
    public String getMessageFormat() { return messageFormat; }
    public String getSampleMessage() { return sampleMessage; }
    public int getMessagesPerSecond() { return messagesPerSecond; }
    public int getTotalMessages() { return totalMessages; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public String getStartedAt() { return startedAt; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(String stoppedAt) { this.stoppedAt = stoppedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public long getSentCount() { return sentCount.get(); }
    public long incrementAndGetSentCount(long delta) { return sentCount.addAndGet(delta); }

    public ScheduledFuture<?> getScheduledFuture() { return scheduledFuture; }
    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) { this.scheduledFuture = scheduledFuture; }

    public boolean isActive() { return status == Status.RUNNING; }
}

