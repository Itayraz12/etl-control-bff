package com.example.service.simulator;

public interface KafkaTopicVerifier {
    boolean topicExists(String environment, String topic);
}

