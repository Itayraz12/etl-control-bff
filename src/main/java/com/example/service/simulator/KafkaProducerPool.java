package com.example.service.simulator;

import com.example.config.KafkaSimulatorProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a pool of KafkaProducers keyed by environment (broker address).
 * Each environment gets one shared producer instance.
 */
@Component
public class KafkaProducerPool {

    private final KafkaSimulatorProperties properties;
    // Key = broker bootstrap address, value = producer that sends byte[] values
    private final Map<String, KafkaProducer<String, byte[]>> producers = new ConcurrentHashMap<>();

    public KafkaProducerPool(KafkaSimulatorProperties properties) {
        this.properties = properties;
    }

    /**
     * Return (and lazily create) a producer for the given environment name.
     *
     * @param environment e.g. "HOME" or "OFFICE"
     * @return KafkaProducer ready to use
     * @throws IllegalArgumentException if the environment is unknown
     */
    public KafkaProducer<String, byte[]> getProducer(String environment) {
        String broker = properties.getBroker(environment);
        if (broker == null) {
            throw new IllegalArgumentException("Unknown Kafka environment: " + environment);
        }
        return producers.computeIfAbsent(broker, this::createProducer);
    }

    /** Validate that the environment name is configured (without creating a producer). */
    public boolean isKnownEnvironment(String environment) {
        return properties.getBroker(environment) != null;
    }

    private KafkaProducer<String, byte[]> createProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 2);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        return new KafkaProducer<>(props);
    }

    @PreDestroy
    public void closeAll() {
        producers.values().forEach(p -> {
            try { p.close(); } catch (Exception ignored) {}
        });
        producers.clear();
    }

    // Allow tests / subclasses to inject a mock producer for a given broker address
    public void registerProducerForTesting(String environment, KafkaProducer<String, byte[]> producer) {
        String broker = properties.getBroker(environment);
        if (broker != null) {
            producers.put(broker, producer);
        }
    }
}

