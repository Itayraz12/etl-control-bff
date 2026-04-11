package com.example.controller;

import com.example.config.UserIdHeaderFilter;
import com.example.model.LoginResult;
import com.example.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .addFilters(new UserIdHeaderFilter())
            .build();
    }

    @Test
    void login_shouldReturnTeamAAndRegularRole_forUserAWithoutUserIdHeader() throws Exception {
        when(authService.login("enc-user-a", "enc-pass-a"))
            .thenReturn(new LoginResult("Team A", "regular"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "enc-user-a",
                      "password": "enc-pass-a"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string("user-role", "regular"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.teamName").value("Team A"))
            .andExpect(jsonPath("$.userRole").doesNotExist());
    }

    @Test
    void login_shouldReturnTeamYardenAndAdminRole_forYarden() throws Exception {
        when(authService.login("enc-user-yarden", "enc-pass-yarden"))
            .thenReturn(new LoginResult("team yarden", "admin"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "enc-user-yarden",
                      "password": "enc-pass-yarden"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string("user-role", "admin"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.teamName").value("team yarden"))
            .andExpect(jsonPath("$.userRole").doesNotExist());
    }

    @Test
    void login_shouldReturnUnauthorized_forInvalidCredentials() throws Exception {
        when(authService.login("bad-user", "bad-pass"))
            .thenThrow(new SecurityException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "bad-user",
                      "password": "bad-pass"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}
