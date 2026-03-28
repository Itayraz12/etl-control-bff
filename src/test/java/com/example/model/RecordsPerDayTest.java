package com.example.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordsPerDayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ------------------------------------------------------------------
    // volumeScore
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} → score {1}")
    @CsvSource({
        "HUNDREDS,          1",
        "THOUSANDS,         2",
        "HUN_THOUSANDS,     3",
        "MILLIONS,          4",
        "TENS_MILLIONS,     5",
        "HUNDREDS_MILLIONS, 6",
    })
    void volumeScore_returnsExpectedValue(RecordsPerDay recordsPerDay, int expectedScore) {
        assertEquals(expectedScore, recordsPerDay.volumeScore());
    }

    // ------------------------------------------------------------------
    // JSON serialisation / deserialisation
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} serialises to \"{1}\"")
    @CsvSource({
        "HUNDREDS,          hundreds",
        "THOUSANDS,         thousands",
        "HUN_THOUSANDS,     hun-thousands",
        "MILLIONS,          millions",
        "TENS_MILLIONS,     tens-millions",
        "HUNDREDS_MILLIONS, hundreds-millions",
    })
    void jsonValue_serialisesToKebabCase(RecordsPerDay recordsPerDay, String expectedJson) throws Exception {
        String json = objectMapper.writeValueAsString(recordsPerDay);
        assertEquals("\"" + expectedJson + "\"", json);
    }

    @ParameterizedTest(name = "\"{0}\" deserialises to {1}")
    @CsvSource({
        "hundreds,          HUNDREDS",
        "thousands,         THOUSANDS",
        "hun-thousands,     HUN_THOUSANDS",
        "millions,          MILLIONS",
        "tens-millions,     TENS_MILLIONS",
        "hundreds-millions, HUNDREDS_MILLIONS",
    })
    void fromValue_deserialisesCaseInsensitive(String jsonValue, RecordsPerDay expected) throws Exception {
        RecordsPerDay result = objectMapper.readValue("\"" + jsonValue + "\"", RecordsPerDay.class);
        assertEquals(expected, result);
    }

    @Test
    void fromValue_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> RecordsPerDay.fromValue("billions"));
    }
}

