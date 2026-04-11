package com.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RecordsPerDay {
    HUNDREDS("hundreds", "Hundreds"),
    THOUSANDS("thousands", "Thousands"),
    HUN_THOUSANDS("hun-thousands", "Hundred of Thousands"),
    MILLIONS("millions", "A Few Millions"),
    TENS_MILLIONS("tens-millions", "Tens of Millions"),
    HUNDREDS_MILLIONS("hundreds-millions", "Hundreds of Millions");

    private final String value;
    private final String label;

    RecordsPerDay(String value, String label) {
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

