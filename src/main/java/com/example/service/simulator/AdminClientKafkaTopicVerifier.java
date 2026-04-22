package com.example.service.simulator;

import com.example.config.KafkaSimulatorProperties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Component
public class AdminClientKafkaTopicVerifier implements KafkaTopicVerifier {

    private static final Logger log = LoggerFactory.getLogger(AdminClientKafkaTopicVerifier.class);

    private final KafkaSimulatorProperties properties;

    public AdminClientKafkaTopicVerifier(KafkaSimulatorProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean topicExists(String environment, String topic) {
        String broker = properties.getBroker(environment);
        if (broker == null) {
            throw new IllegalArgumentException("Unknown Kafka environment: " + environment);
        }

        if (!StringUtils.hasText(broker)) {
            throw new IllegalStateException("Kafka broker address for environment '" + environment + "' is empty or not configured");
        }

        log.debug("Testing Kafka connection to broker '{}' for environment '{}', topic '{}'", broker, environment, topic);

        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");

        try (AdminClient adminClient = AdminClient.create(config)) {
            boolean exists = adminClient.listTopics().names().get(5, TimeUnit.SECONDS).contains(topic);
            log.debug("Topic '{}' exists: {}", topic, exists);
            return exists;
        } catch (Exception ex) {
            String errorMsg = String.format(
                "Failed to connect to Kafka broker '%s' or list topics: %s. " +
                "Check that (1) the broker address is correct, (2) the broker is running and reachable, and (3) network connectivity is available.",
                broker, ex.getMessage());
            log.error(errorMsg, ex);
            throw new IllegalStateException(errorMsg, ex);
        }
    }
}

