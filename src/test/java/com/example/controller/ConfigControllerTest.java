package com.example.controller;

import com.example.config.UserIdHeaderFilter;
import com.example.model.ConfigOption;
import com.example.model.Filter;
import com.example.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfigControllerTest {

    private static final String USER_ID = "ui-user-123";

    private MockMvc mockMvc;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        ConfigController controller = new ConfigController();
        configService = mock(ConfigService.class);
        ReflectionTestUtils.setField(controller, "configService", configService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .addFilters(new UserIdHeaderFilter())
            .build();
    }

    @Test
    void getStreamingContinuities_shouldReturnOrderedOptions() throws Exception {
        when(configService.getStreamingContinuities()).thenReturn(List.of(
            new ConfigOption("once", "Once"),
            new ConfigOption("continuous", "Continuous")
        ));

        mockMvc.perform(withUserId(get("/api/config/streaming-continuities")))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [
                  {"value":"once","label":"Once"},
                  {"value":"continuous","label":"Continuous"}
                ]
                """, true));
    }

    @Test
    void getRecordsPerDayOptions_shouldReturnOrderedOptions() throws Exception {
        when(configService.getRecordsPerDayOptions()).thenReturn(List.of(
            new ConfigOption("hundreds", "Hundreds"),
            new ConfigOption("millions", "A Few Millions")
        ));

        mockMvc.perform(withUserId(get("/api/config/records-per-day")))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [
                  {"value":"hundreds","label":"Hundreds"},
                  {"value":"millions","label":"A Few Millions"}
                ]
                """, true));
    }

    @Test
    void getFilters_shouldUseEnvironmentRequestParam() throws Exception {
        when(configService.getFilters("prod")).thenReturn(List.of(
            new Filter("filter-001", "equals", null, "Matches records", "team-a",
                "s3://etl-control/filters/equals/v1/filter.py", true, true, true, "1.0.0", List.of())
        ));

        mockMvc.perform(withUserId(get("/api/config/filters")
                .param("environment", "prod")))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [
                  {"_id":"filter-001","name":"equals","description":"Matches records"}
                ]
                """, false));
    }

    @Test
    void getStreamingContinuities_shouldRejectWhenUserIdHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/config/streaming-continuities"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Missing required header 'X-user-id'"));
    }

    @Test
    void getFilters_shouldRejectWhenUserIdHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/config/filters")
                .param("environment", "prod"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Missing required header 'X-user-id'"));
    }

    @Test
    void getFilters_shouldRejectBlankEnvironment() throws Exception {
        when(configService.getFilters(" ")).thenThrow(new IllegalArgumentException("environment must not be empty"));

        mockMvc.perform(withUserId(get("/api/config/filters")
                .param("environment", " ")))
            .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder withUserId(MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder.header(UserIdHeaderFilter.HEADER_NAME, USER_ID);
    }
}
