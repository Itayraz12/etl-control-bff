package com.example.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamingContinuityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ------------------------------------------------------------------
    // frequencyScore
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} → score {1}")
    @CsvSource({
        "ONCE,            1",
        "EVERY_DAY,       2",
        "EVERY_FEW_HOURS, 3",
        "EVERY_HOUR,      4",
        "CONTINUOUS,      5",
    })
    void frequencyScore_returnsExpectedValue(StreamingContinuity continuity, int expectedScore) {
        assertEquals(expectedScore, continuity.frequencyScore());
    }

    // ------------------------------------------------------------------
    // JSON serialisation / deserialisation
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} serialises to \"{1}\"")
    @CsvSource({
        "ONCE,            once",
        "EVERY_HOUR,      every-hour",
        "EVERY_FEW_HOURS, every-few-hours",
        "EVERY_DAY,       every-day",
        "CONTINUOUS,      continuous",
    })
    void jsonValue_serialisesToKebabCase(StreamingContinuity continuity, String expectedJson) throws Exception {
        String json = objectMapper.writeValueAsString(continuity);
        assertEquals("\"" + expectedJson + "\"", json);
    }

    @ParameterizedTest(name = "\"{0}\" deserialises to {1}")
    @CsvSource({
        "once,            ONCE",
        "every-hour,      EVERY_HOUR",
        "every-few-hours, EVERY_FEW_HOURS",
        "every-day,       EVERY_DAY",
        "continuous,      CONTINUOUS",
    })
    void fromValue_deserialisesCaseInsensitive(String jsonValue, StreamingContinuity expected) throws Exception {
        StreamingContinuity result = objectMapper.readValue("\"" + jsonValue + "\"", StreamingContinuity.class);
        assertEquals(expected, result);
    }

    @Test
    void fromValue_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> StreamingContinuity.fromValue("weekly"));
    }
}

