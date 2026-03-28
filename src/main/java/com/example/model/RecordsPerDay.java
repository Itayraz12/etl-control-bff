package com.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RecordsPerDay {
    HUNDREDS("hundreds"),
    THOUSANDS("thousands"),
    HUN_THOUSANDS("hun-thousands"),
    MILLIONS("millions"),
    TENS_MILLIONS("tens-millions"),
    HUNDREDS_MILLIONS("hundreds-millions");

    private final String value;

    RecordsPerDay(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RecordsPerDay fromValue(String value) {
        for (RecordsPerDay rpd : values()) {
            if (rpd.value.equalsIgnoreCase(value)) {
                return rpd;
            }
        }
        throw new IllegalArgumentException("Unknown RecordsPerDay: " + value);
    }

    /** Volume score used for volume calculation (higher = more data). */
    public int volumeScore() {
        return switch (this) {
            case HUNDREDS          -> 1;
            case THOUSANDS         -> 2;
            case HUN_THOUSANDS     -> 3;
            case MILLIONS          -> 4;
            case TENS_MILLIONS     -> 5;
            case HUNDREDS_MILLIONS -> 6;
        };
    }
}

