package com.example.service.simulator;

import com.example.model.KafkaSimulatorDtos.StartTaskRequest;
import com.example.model.KafkaSimulatorDtos.StopTaskResponse;
import com.example.model.KafkaSimulatorDtos.TaskStatusResponse;
import com.example.model.KafkaSimulatorDtos.TaskSummary;
import com.example.model.KafkaSimulatorDtos.TestConnectionRequest;
import com.example.model.KafkaSimulatorDtos.TestConnectionResponse;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class KafkaSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(KafkaSimulatorService.class);

    private final KafkaProducerPool producerPool;
    private final KafkaTopicVerifier topicVerifier;
    private final ScheduledExecutorService scheduler;

    /** In-memory task registry keyed by taskId */
    private final Map<String, KafkaSimulatorTask> taskRegistry = new ConcurrentHashMap<>();

    @Autowired
    public KafkaSimulatorService(KafkaProducerPool producerPool, KafkaTopicVerifier topicVerifier) {
        this.producerPool = producerPool;
        this.topicVerifier = topicVerifier;
        this.scheduler = Executors.newScheduledThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
    }

    // constructor for tests that supply a custom scheduler
    public KafkaSimulatorService(KafkaProducerPool producerPool, ScheduledExecutorService scheduler) {
        this(producerPool, scheduler, (environment, topic) -> true);
    }

    // constructor for tests that supply a custom scheduler and custom verifier
    public KafkaSimulatorService(KafkaProducerPool producerPool, ScheduledExecutorService scheduler, KafkaTopicVerifier topicVerifier) {
        this.producerPool = producerPool;
        this.topicVerifier = topicVerifier;
        this.scheduler = scheduler;
    }

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    public KafkaSimulatorTask startTask(StartTaskRequest request) {
        validateRequest(request);

        String taskId = UUID.randomUUID().toString();
        String startedAt = Instant.now().toString();

        KafkaSimulatorTask task = new KafkaSimulatorTask(
            taskId,
            request.getEnvironment(),
            request.getTopic(),
            request.getMessageFormat(),
            request.getSampleMessage(),
            request.getMessagesPerSecond(),
            request.getTotalMessages(),
            request.getIntervalSeconds(),
            startedAt
        );

        taskRegistry.put(taskId, task);
        scheduleTask(task);
        return task;
    }

    public StopTaskResponse stopTask(String taskId) {
        KafkaSimulatorTask task = taskRegistry.get(taskId);
        if (task == null || !task.isActive()) {
            return null; // signals 404 to controller
        }
        cancelTask(task, KafkaSimulatorTask.Status.STOPPED);
        return new StopTaskResponse(taskId, "stopped", task.getStoppedAt());
    }

    /**
     * Deletes a task. If it is still running it is stopped first.
     *
     * @return true if deleted, false if not found
     */
    public boolean deleteTask(String taskId) {
        KafkaSimulatorTask task = taskRegistry.remove(taskId);
        if (task == null) {
            return false;
        }
        if (task.isActive()) {
            cancelTask(task, KafkaSimulatorTask.Status.STOPPED);
        }
        return true;
    }

    public List<TaskSummary> listTasks() {
        return taskRegistry.values().stream()
            .sorted(Comparator.comparing(KafkaSimulatorTask::getStartedAt).reversed())
            .map(this::toSummary)
            .collect(Collectors.toList());
    }

    public TaskStatusResponse getTaskStatus(String taskId) {
        KafkaSimulatorTask task = taskRegistry.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found");
        }

        String status = task.getStatus().name().toLowerCase();
        long sentCount = task.getSentCount();
        return new TaskStatusResponse(taskId, status, sentCount, buildStatusMessage(task));
    }

    public TestConnectionResponse testConnection(TestConnectionRequest request) {
        validateTestConnectionRequest(request);
        boolean exists = topicVerifier.topicExists(request.getEnvironment(), request.getTopic().trim());
        if (!exists) {
            throw new TopicNotFoundException("Topic not found: " + request.getTopic().trim());
        }
        return new TestConnectionResponse(true, "Connection successful and topic exists");
    }

    // -------------------------------------------------------------------------
    // INTERNAL SCHEDULING
    // -------------------------------------------------------------------------

    private void scheduleTask(KafkaSimulatorTask task) {
        if (task.getIntervalSeconds() == 0) {
            // Fire once, immediately
            ScheduledFuture<?> future = scheduler.schedule(
                () -> executeBurst(task, true), 0, TimeUnit.SECONDS);
            task.setScheduledFuture(future);
        } else {
            // Repeat every intervalSeconds
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> executeBurst(task, false),
                0,
                task.getIntervalSeconds(),
                TimeUnit.SECONDS);
            task.setScheduledFuture(future);
        }
    }

    /**
     * Sends one burst of {@code messagesPerSecond} messages to Kafka.
     *
     * @param autoComplete when true, marks the task COMPLETED after the burst
     *                     (used for intervalSeconds == 0 tasks)
     */
    private void executeBurst(KafkaSimulatorTask task, boolean autoComplete) {
        if (!task.isActive()) {
            return;
        }

        int batchSize = task.getMessagesPerSecond();
        int totalLimit = task.getTotalMessages();

        // How many remain?
        long alreadySent = task.getSentCount();
        int toSend;
        if (totalLimit == -1) {
            toSend = batchSize;
        } else {
            long remaining = totalLimit - alreadySent;
            if (remaining <= 0) {
                cancelTask(task, KafkaSimulatorTask.Status.COMPLETED);
                return;
            }
            toSend = (int) Math.min(batchSize, remaining);
        }

        try {
            KafkaProducer<String, byte[]> producer = producerPool.getProducer(task.getEnvironment());
            for (int i = 0; i < toSend; i++) {
                if (!task.isActive()) break;
                String rendered = MessageRenderer.substitute(task.getSampleMessage());
                byte[] payload = MessageRenderer.toBytes(rendered, task.getMessageFormat());
                ProducerRecord<String, byte[]> record = new ProducerRecord<>(task.getTopic(), payload);
                producer.send(record, (metadata, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to send message for task {}: {}", task.getTaskId(), ex.getMessage());
                    }
                });
            }
            long sent = task.incrementAndGetSentCount(toSend);
            log.debug("Task {} sent {} messages (total so far: {})", task.getTaskId(), toSend, sent);

            // Check if we've hit the total limit after this burst
            if (totalLimit != -1 && sent >= totalLimit) {
                cancelTask(task, KafkaSimulatorTask.Status.COMPLETED);
                return;
            }
        } catch (Exception ex) {
            log.error("Task {} encountered an error: {}", task.getTaskId(), ex.getMessage(), ex);
            task.setLastError(ex.getMessage());
            cancelTask(task, KafkaSimulatorTask.Status.ERROR);
            return;
        }

        if (autoComplete) {
            cancelTask(task, KafkaSimulatorTask.Status.COMPLETED);
        }
    }

    private void cancelTask(KafkaSimulatorTask task, KafkaSimulatorTask.Status finalStatus) {
        ScheduledFuture<?> future = task.getScheduledFuture();
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        task.setStatus(finalStatus);
        task.setStoppedAt(Instant.now().toString());
    }

    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    private void validateRequest(StartTaskRequest request) {
        if (!StringUtils.hasText(request.getEnvironment())) {
            throw new IllegalArgumentException("environment is required");
        }
        if (!producerPool.isKnownEnvironment(request.getEnvironment())) {
            throw new IllegalArgumentException("Unknown Kafka environment: " + request.getEnvironment());
        }
        if (!StringUtils.hasText(request.getTopic())) {
            throw new IllegalArgumentException("topic is required");
        }
        if (!StringUtils.hasText(request.getMessageFormat())) {
            throw new IllegalArgumentException("messageFormat is required");
        }
        if (request.getSampleMessage() == null) {
            throw new IllegalArgumentException("sampleMessage is required");
        }
        if (request.getMessagesPerSecond() == null || request.getMessagesPerSecond() < 1 || request.getMessagesPerSecond() > 10000) {
            throw new IllegalArgumentException("messagesPerSecond must be between 1 and 10000");
        }
        if (request.getTotalMessages() == null || (request.getTotalMessages() < 1 && request.getTotalMessages() != -1)) {
            throw new IllegalArgumentException("totalMessages must be >= 1 or -1 for unlimited");
        }
        if (request.getIntervalSeconds() == null || request.getIntervalSeconds() < 0) {
            throw new IllegalArgumentException("intervalSeconds must be >= 0");
        }
    }

    private void validateTestConnectionRequest(TestConnectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!StringUtils.hasText(request.getEnvironment())) {
            throw new IllegalArgumentException("environment is required");
        }
        if (!producerPool.isKnownEnvironment(request.getEnvironment())) {
            throw new IllegalArgumentException("Unknown Kafka environment: " + request.getEnvironment());
        }
        if (!StringUtils.hasText(request.getTopic())) {
            throw new IllegalArgumentException("topic is required");
        }
    }

    // -------------------------------------------------------------------------
    // MAPPING
    // -------------------------------------------------------------------------

    private TaskSummary toSummary(KafkaSimulatorTask task) {
        return new TaskSummary(
            task.getTaskId(),
            task.getEnvironment(),
            task.getTopic(),
            task.getMessageFormat(),
            task.getMessagesPerSecond(),
            task.getTotalMessages(),
            task.getIntervalSeconds(),
            task.getStatus().name().toLowerCase(),
            task.getSentCount(),
            task.getStartedAt(),
            task.getStoppedAt()
        );
    }

    private String buildStatusMessage(KafkaSimulatorTask task) {
        String status = task.getStatus().name().toLowerCase();
        long sentCount = task.getSentCount();

        return switch (status) {
            case "running" -> "Published " + sentCount + " messages so far";
            case "completed" -> {
                if (task.getTotalMessages() == -1) {
                    yield "Completed successfully: sent " + sentCount + " messages";
                }
                yield "Completed successfully: sent " + sentCount + "/" + task.getTotalMessages() + " messages";
            }
            case "stopped" -> "Stopped by user after sending " + sentCount + " messages";
            case "error" -> {
                if (StringUtils.hasText(task.getLastError())) {
                    yield "Task failed after sending " + sentCount + " messages: " + task.getLastError();
                }
                yield "Task failed after sending " + sentCount + " messages";
            }
            default -> "Unknown task status";
        };
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

