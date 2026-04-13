package com.example.controller;

import com.example.config.UserIdHeaderFilter;
import com.example.model.Deployment;
import com.example.service.FilterEvaluationService;
import com.example.service.BackendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackendControllerTest {

    private static final String USER_ID = "ui-user-123";

    private MockMvc mockMvc;
    private BackendService backendService;
    private FilterEvaluationService filterEvaluationService;

    @BeforeEach
    void setUp() {
        BackendController controller = new BackendController();
        backendService = mock(BackendService.class);
        filterEvaluationService = mock(FilterEvaluationService.class);
        ReflectionTestUtils.setField(controller, "backendService", backendService);
        ReflectionTestUtils.setField(controller, "filterEvaluationService", filterEvaluationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .addFilters(new UserIdHeaderFilter())
            .build();
    }

    @Test
    void testKafkaConnection_shouldReturnSuccess() throws Exception {
        when(backendService.testKafkaConnection("topic-1", "staging")).thenReturn("success");

        mockMvc.perform(withUserId(get("/api/backend/kafka/test-connection"))
                .param("topicName", "topic-1")
                .param("environment", "staging"))
            .andExpect(status().isOk())
            .andExpect(content().string("success"));
    }

    @Test
    void testKafkaConnection_shouldReturnBadRequestWhenTopicIsBlank() throws Exception {
        when(backendService.testKafkaConnection("", "staging"))
            .thenThrow(new IllegalArgumentException("topicName must not be empty"));

        mockMvc.perform(withUserId(get("/api/backend/kafka/test-connection"))
                .param("topicName", "")
                .param("environment", "staging"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("error: topicName must not be empty"));
    }

    @Test
    void testRabbitMqConnection_shouldReturnSuccess() throws Exception {
        when(backendService.testRabbitMqConnection("/vhost", "5672", "ingest", "1.1.1.1", "guest", "123456", "dev"))
            .thenReturn("success");

        mockMvc.perform(withUserId(get("/api/backend/rabbitmq/test-connection"))
                .param("vhost", "/vhost")
                .param("port", "5672")
                .param("queueName", "ingest")
                .param("ip", "1.1.1.1")
                .param("username", "guest")
                .param("password", "123456")
                .param("environment", "dev"))
            .andExpect(status().isOk())
            .andExpect(content().string("success"));
    }

    @Test
    void testRabbitMqConnection_shouldReturnBadRequestWhenPortIsInvalid() throws Exception {
        when(backendService.testRabbitMqConnection("/vhost", "abc", "ingest", "1.1.1.1", "guest", "123456", "dev"))
            .thenThrow(new IllegalArgumentException("port must be a valid integer"));

        mockMvc.perform(withUserId(get("/api/backend/rabbitmq/test-connection"))
                .param("vhost", "/vhost")
                .param("port", "abc")
                .param("queueName", "ingest")
                .param("ip", "1.1.1.1")
                .param("username", "guest")
                .param("password", "123456")
                .param("environment", "dev"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("error: port must be a valid integer"));
    }

    @Test
    void getConfigurationYaml_shouldReturnRequestedMessageWhenConfigurationIsMissing() throws Exception {
        when(backendService.getConfigurationYaml("analytics", "gitlab", "team-a", "staging", false))
            .thenThrow(new IllegalArgumentException("missing config"));

        mockMvc.perform(withUserId(get("/api/backend/configuration/yaml"))
                .param("productType", "analytics")
                .param("source", "gitlab")
                .param("team", "team-a")
                .param("environment", "staging"))
            .andExpect(status().isNotFound())
            .andExpect(content().string("eyal is here"));
    }

    @Test
    void getConfigurationYaml_shouldReturnRequestedMessageWhenServiceFails() throws Exception {
        when(backendService.getConfigurationYaml("analytics", "gitlab", "team-a", "staging", false))
            .thenThrow(new IllegalStateException("service failure"));

        mockMvc.perform(withUserId(get("/api/backend/configuration/yaml"))
                .param("productType", "analytics")
                .param("source", "gitlab")
                .param("team", "team-a")
                .param("environment", "staging"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("eyal is here"));
    }

    @Test
    void getDeployments_shouldReturnAllTeamsWhenTeamNameIsMissing() throws Exception {
        when(backendService.getAllDeployments()).thenReturn(List.of(
            new Deployment("1", "Team A", "Data Pipeline", "GitHub", "draft", "1.0.0", "1.0.0", 1L, 1L, "staging"),
            new Deployment("2", "Team B", "Analytics4", "GitLab", "running", "3.0.1", "2.9.0", 2L, 2L, "production")
        ));

        mockMvc.perform(withUserId(get("/api/backend/deployments")))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [
                  {"id":"1","teamName":"Team A","productType":"Data Pipeline"},
                  {"id":"2","teamName":"Team B","productType":"Analytics4"}
                ]
                """, false));
    }

    @Test
    void getDeployments_shouldReturnTeamDeploymentsWhenTeamNameIsProvided() throws Exception {
        when(backendService.getDeployments("Team A")).thenReturn(List.of(
            new Deployment("1", "Team A", "Data Pipeline", "GitHub", "draft", "1.0.0", "1.0.0", 1L, 1L, "staging")
        ));

        mockMvc.perform(withUserId(get("/api/backend/deployments")
                .param("teamName", "Team A")))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [
                  {"id":"1","teamName":"Team A","productType":"Data Pipeline"}
                ]
                """, false));
    }

    @Test
    void testKafkaConnection_shouldRejectRequestWhenUserIdHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/backend/kafka/test-connection")
                .param("topicName", "topic-1")
                .param("environment", "staging"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Missing required header 'X-user-id'"));
    }

    @Test
    void testKafkaConnection_shouldPrintUserIdToSystemOut() throws Exception {
        when(backendService.testKafkaConnection("topic-1", "staging")).thenReturn("success");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));

            mockMvc.perform(withUserId(get("/api/backend/kafka/test-connection"))
                    .param("topicName", "topic-1")
                    .param("environment", "staging"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        } finally {
            System.setOut(originalOut);
        }

        String consoleOutput = outputStream.toString(StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(
            consoleOutput.contains("Request received [userId=" + USER_ID),
            () -> "Expected console output to contain the request user id, but was: " + consoleOutput);
    }

    @Test
    void getSchemaFromPayload_shouldPassFormatTypeFromPath() throws Exception {
        when(backendService.getSchemaFromPayload("payload-body", "CSV"))
            .thenReturn("{\"title\":\"Person\"}");

        mockMvc.perform(withUserId(post("/api/backend/schemaByExample/CSV")
                .contentType("application/json")
                .content("payload-body")))
            .andExpect(status().isOk())
            .andExpect(content().string("{\"title\":\"Person\"}"));
    }

    @Test
    void getSchemaFromPayload_shouldReturnBadRequestForUnsupportedFormatType() throws Exception {
        when(backendService.getSchemaFromPayload("payload-body", "XML"))
            .thenThrow(new IllegalArgumentException("Unsupported formatType: XML"));

        mockMvc.perform(withUserId(post("/api/backend/schemaByExample/XML")
                .contentType("application/json")
                .content("payload-body")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("error: Unsupported formatType: XML"));
    }

    @Test
    void evaluateFilters_shouldReturnBooleanMatchResult() throws Exception {
        when(filterEvaluationService.evaluate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(true);

        mockMvc.perform(withUserId(post("/api/backend/filters/evaluate")
                .contentType("application/json")
                .content("""
                    {
                      "configuration": {
                        "filters": {
                          "config": [
                            {
                              "rule": {
                                "and": [
                                  {
                                    "field": "firstName",
                                    "op": "EQUALS",
                                    "values": ["john"]
                                  }
                                ]
                              }
                            }
                          ]
                        }
                      },
                      "inputFields": {
                        "firstName": "john"
                      }
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"matches": true}
                """, true));
    }

    private MockHttpServletRequestBuilder withUserId(MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder.header(UserIdHeaderFilter.HEADER_NAME, USER_ID);
    }
}
