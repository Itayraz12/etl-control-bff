package com.example.service.simulator;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles per-message placeholder substitution and format-aware serialization.
 */
public class MessageRenderer {

    private MessageRenderer() {}

    /**
     * Substitute {{uuid}}, {{value}}, {{now}} in the template — once per call so
     * every message gets a fresh set of values.
     */
    public static String substitute(String template) {
        String uuid = UUID.randomUUID().toString();
        String value = String.valueOf(ThreadLocalRandom.current().nextInt(1, 1001));
        String now = Instant.now().toString(); // ISO-8601 UTC, e.g. 2026-04-22T10:35:00.000Z
        return template
            .replace("{{uuid}}", uuid)
            .replace("{{value}}", value)
            .replace("{{now}}", now);
    }

    /**
     * Convert a rendered message to the byte[] that will be published to Kafka.
     * For "protobuf" the template is treated as base64-encoded binary; for all
     * other formats the UTF-8 string is used directly.
     */
    public static byte[] toBytes(String renderedMessage, String messageFormat) {
        if ("protobuf".equalsIgnoreCase(messageFormat)) {
            try {
                return Base64.getDecoder().decode(renderedMessage.trim());
            } catch (IllegalArgumentException ex) {
                // Fall back to UTF-8 if the payload is not valid base64
                return renderedMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return renderedMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}

