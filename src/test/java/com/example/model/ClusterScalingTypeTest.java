package com.example.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ClusterScalingTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest(name = "{0} serialises to \"{1}\"")
    @CsvSource({
        "DYNAMIC, dynamic",
        "FIXED,   fixed",
    })
    void jsonValue_serialisesToLowerCase(ClusterScalingType scalingType, String expectedJson) throws Exception {
        String json = objectMapper.writeValueAsString(scalingType);
        assertEquals("\"" + expectedJson + "\"", json);
    }

    @ParameterizedTest(name = "\"{0}\" deserialises to {1}")
    @CsvSource({
        "dynamic, DYNAMIC",
        "fixed,   FIXED",
        "DYNAMIC, DYNAMIC",
        "FIXED,   FIXED",
    })
    void fromValue_deserialisesCaseInsensitive(String jsonValue, ClusterScalingType expected) throws Exception {
        ClusterScalingType result = objectMapper.readValue("\"" + jsonValue + "\"", ClusterScalingType.class);
        assertEquals(expected, result);
    }

    @Test
    void fromValue_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ClusterScalingType.fromValue("auto"));
    }
}

