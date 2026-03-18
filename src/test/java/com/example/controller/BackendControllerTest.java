package com.example.controller;

import com.example.service.BackendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackendControllerTest {

    private MockMvc mockMvc;
    private BackendService backendService;

    @BeforeEach
    void setUp() {
        BackendController controller = new BackendController();
        backendService = mock(BackendService.class);
        ReflectionTestUtils.setField(controller, "backendService", backendService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testKafkaConnection_shouldReturnSuccess() throws Exception {
        when(backendService.testKafkaConnection("topic-1", "staging")).thenReturn("success");

        mockMvc.perform(get("/api/backend/kafka/test-connection")
                .param("topicName", "topic-1")
                .param("environment", "staging"))
            .andExpect(status().isOk())
            .andExpect(content().string("success"));
    }

    @Test
    void testKafkaConnection_shouldReturnBadRequestWhenTopicIsBlank() throws Exception {
        when(backendService.testKafkaConnection("", "staging"))
            .thenThrow(new IllegalArgumentException("topicName must not be empty"));

        mockMvc.perform(get("/api/backend/kafka/test-connection")
                .param("topicName", "")
                .param("environment", "staging"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("error: topicName must not be empty"));
    }
}

