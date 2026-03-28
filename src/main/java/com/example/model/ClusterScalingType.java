package com.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Describes whether a Flink cluster scales its parallelism dynamically
 * (up to a maximum) or runs at a fixed parallelism regardless of load.
 */
public enum ClusterScalingType {

    /** Cluster parallelism scales up automatically; the configured value is the maximum. */
    DYNAMIC("dynamic"),

    /** Cluster parallelism is pinned to its configured value at all times. */
    FIXED("fixed");

    private final String value;

    ClusterScalingType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ClusterScalingType fromValue(String value) {
        for (ClusterScalingType t : values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown ClusterScalingType: " + value);
    }
}

