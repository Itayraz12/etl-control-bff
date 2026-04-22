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
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KafkaSimulatorPayloadTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Build properties with HOME1 environment
        KafkaSimulatorProperties props = new KafkaSimulatorProperties();
        Map<String, String> envs = new HashMap<>();
        envs.put("HOME1", "localhost:9092");
        props.setEnvironments(envs);

        // Create mock Kafka producer
        @SuppressWarnings("unchecked")
        KafkaProducer<String, byte[]> mockProducer = (KafkaProducer<String, byte[]>) mock(KafkaProducer.class);
        RecordMetadata meta = new RecordMetadata(new TopicPartition("test__topic", 0), 0, 0, 0, 0, 0);
        when(mockProducer.send(any(), any())).thenAnswer(inv -> {
            org.apache.kafka.clients.producer.Callback cb = inv.getArgument(1);
            cb.onCompletion(meta, null);
            return CompletableFuture.completedFuture(meta);
        });

        KafkaProducerPool producerPool = new KafkaProducerPool(props);
        producerPool.registerProducerForTesting("HOME1", mockProducer);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        KafkaTopicVerifier topicVerifier = mock(KafkaTopicVerifier.class);
        KafkaSimulatorService simulatorService = new KafkaSimulatorService(producerPool, scheduler, topicVerifier);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new KafkaSimulatorController(simulatorService))
            .build();
    }

    @Test
    void testStartWithProvidedPayload() throws Exception {
        String payload = """
            {
              "environment": "HOME1",
              "topic": "test__topic",
              "messageFormat": "json",
              "sampleMessage": "{\\n  \\"id\\": \\"{{uuid}}\\",\\n  \\"timestamp\\": \\"{{now}}\\",\\n  \\"value\\": \\"{{value}}\\"\\n}",
              "messagesPerSecond": 1,
              "totalMessages": 10,
              "intervalSeconds": 1
            }
            """;

        mockMvc.perform(post("/api/simulator/kafka/start")
                .header("X-User-Id", "test-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("running"))
            .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }
}

