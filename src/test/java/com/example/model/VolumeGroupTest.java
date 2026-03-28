package com.example.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VolumeGroupTest {

    // ------------------------------------------------------------------
    // Full matrix — every combination mapped to expected group
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
        // ONCE (freq=1)
        "ONCE, HUNDREDS,          LOW",
        "ONCE, THOUSANDS,         LOW",
        "ONCE, HUN_THOUSANDS,     LOW",
        "ONCE, MILLIONS,          MEDIUM",
        "ONCE, TENS_MILLIONS,     MEDIUM",
        "ONCE, HUNDREDS_MILLIONS, MEDIUM",
        // EVERY_DAY (freq=2)
        "EVERY_DAY, HUNDREDS,          LOW",
        "EVERY_DAY, THOUSANDS,         LOW",
        "EVERY_DAY, HUN_THOUSANDS,     MEDIUM",
        "EVERY_DAY, MILLIONS,          MEDIUM",
        "EVERY_DAY, TENS_MILLIONS,     MEDIUM",
        "EVERY_DAY, HUNDREDS_MILLIONS, HIGH",
        // EVERY_FEW_HOURS (freq=3)
        "EVERY_FEW_HOURS, HUNDREDS,          LOW",
        "EVERY_FEW_HOURS, THOUSANDS,         MEDIUM",
        "EVERY_FEW_HOURS, HUN_THOUSANDS,     MEDIUM",
        "EVERY_FEW_HOURS, MILLIONS,          MEDIUM",
        "EVERY_FEW_HOURS, TENS_MILLIONS,     HIGH",
        "EVERY_FEW_HOURS, HUNDREDS_MILLIONS, HIGH",
        // EVERY_HOUR (freq=4)
        "EVERY_HOUR, HUNDREDS,          MEDIUM",
        "EVERY_HOUR, THOUSANDS,         MEDIUM",
        "EVERY_HOUR, HUN_THOUSANDS,     MEDIUM",
        "EVERY_HOUR, MILLIONS,          HIGH",
        "EVERY_HOUR, TENS_MILLIONS,     HIGH",
        "EVERY_HOUR, HUNDREDS_MILLIONS, HIGH",
        // CONTINUOUS (freq=5)
        "CONTINUOUS, HUNDREDS,          MEDIUM",
        "CONTINUOUS, THOUSANDS,         MEDIUM",
        "CONTINUOUS, HUN_THOUSANDS,     HIGH",
        "CONTINUOUS, MILLIONS,          HIGH",
        "CONTINUOUS, TENS_MILLIONS,     HIGH",
        "CONTINUOUS, HUNDREDS_MILLIONS, HIGH",
    })
    void calculate_fullMatrix(StreamingContinuity continuity, RecordsPerDay recordsPerDay, VolumeGroup expected) {
        assertEquals(expected, VolumeGroup.calculate(continuity, recordsPerDay));
    }

    // ------------------------------------------------------------------
    // Boundary / representative spot-checks
    // ------------------------------------------------------------------

    @Test
    void calculate_lowestPossibleScore_returnsLow() {
        // score = 1 + 1 = 2
        assertEquals(VolumeGroup.LOW, VolumeGroup.calculate(StreamingContinuity.ONCE, RecordsPerDay.HUNDREDS));
    }

    @Test
    void calculate_scoreExactlyAtLowBoundary_returnsLow() {
        // score = 2 + 2 = 4 (last LOW)
        assertEquals(VolumeGroup.LOW, VolumeGroup.calculate(StreamingContinuity.EVERY_DAY, RecordsPerDay.THOUSANDS));
    }

    @Test
    void calculate_scoreJustAboveLowBoundary_returnsMedium() {
        // score = 2 + 3 = 5 (first MEDIUM)
        assertEquals(VolumeGroup.MEDIUM, VolumeGroup.calculate(StreamingContinuity.EVERY_DAY, RecordsPerDay.HUN_THOUSANDS));
    }

    @Test
    void calculate_scoreExactlyAtMediumBoundary_returnsMedium() {
        // score = 4 + 3 = 7 (last MEDIUM)
        assertEquals(VolumeGroup.MEDIUM, VolumeGroup.calculate(StreamingContinuity.EVERY_HOUR, RecordsPerDay.HUN_THOUSANDS));
    }

    @Test
    void calculate_scoreJustAboveMediumBoundary_returnsHigh() {
        // score = 4 + 4 = 8 (first HIGH)
        assertEquals(VolumeGroup.HIGH, VolumeGroup.calculate(StreamingContinuity.EVERY_HOUR, RecordsPerDay.MILLIONS));
    }

    @Test
    void calculate_highestPossibleScore_returnsHigh() {
        // score = 5 + 6 = 11
        assertEquals(VolumeGroup.HIGH, VolumeGroup.calculate(StreamingContinuity.CONTINUOUS, RecordsPerDay.HUNDREDS_MILLIONS));
    }
}

