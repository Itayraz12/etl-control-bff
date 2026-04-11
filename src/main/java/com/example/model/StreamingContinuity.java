package com.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StreamingContinuity {
    ONCE("once", "Once"),
    EVERY_HOUR("every-hour", "Every Hour"),
    EVERY_FEW_HOURS("every-few-hours", "Every Few Hours"),
    EVERY_DAY("every-day", "Once a Day"),
    CONTINUOUS("continuous", "Continuous");

    private final String value;
    private final String label;

    StreamingContinuity(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static StreamingContinuity fromValue(String value) {
        for (StreamingContinuity sc : values()) {
            if (sc.value.equalsIgnoreCase(value)) {
                return sc;
            }
        }
        throw new IllegalArgumentException("Unknown StreamingContinuity: " + value);
    }

    /** Frequency score used for volume calculation (higher = more frequent). */
    public int frequencyScore() {
        return switch (this) {
            case ONCE            -> 1;
            case EVERY_DAY       -> 2;
            case EVERY_FEW_HOURS -> 3;
            case EVERY_HOUR      -> 4;
            case CONTINUOUS      -> 5;
        };
    }
}

