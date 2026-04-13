package com.example.controller;

import com.example.config.AdminAuthorizationFilter;
import com.example.config.UserIdHeaderFilter;
import com.example.service.AuthService;
import com.example.service.admin.AdminManagementService;
import com.example.service.admin.AdminRepositories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminManagementControllerTest {

    private static final String ADMIN_USER_ID = "yarden";
    private static final String REGULAR_USER_ID = "a";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminRepositories.AdminTeamRepository teamRepository = new AdminRepositories.AdminTeamRepository();
        AdminRepositories.AdminUserRepository userRepository = new AdminRepositories.AdminUserRepository();
        AdminManagementService adminManagementService = new AdminManagementService(teamRepository, userRepository);
        AuthService authService = new AuthService("MDEyMzQ1Njc4OWFiY2RlZg==");

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminManagementController(adminManagementService))
            .addFilters(new UserIdHeaderFilter(), new AdminAuthorizationFilter(authService))
            .build();
    }

    @Test
    void getTeams_shouldReturnUnauthorizedWhenHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/backend/admin/teams"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().json("""
                {"message":"Unauthenticated user"}
                """, true));
    }

    @Test
    void getTeams_shouldReturnForbiddenForRegularUser() throws Exception {
        mockMvc.perform(withUserId(get("/api/backend/admin/teams"), REGULAR_USER_ID))
            .andExpect(status().isForbidden())
            .andExpect(content().json("""
                {"message":"Admin access is required"}
                """, true));
    }

    @Test
    void getTeams_shouldReturnSeededTeamsForAdmin() throws Exception {
        mockMvc.perform(withUserId(get("/api/backend/admin/teams"), ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].teamName").value("Team A"))
            .andExpect(jsonPath("$[0].devopsName").value("platform-devops"));
    }

    @Test
    void createTeam_shouldPersistAndReturnCreatedEntity() throws Exception {
        mockMvc.perform(withUserId(post("/api/backend/admin/teams"), ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "teamName": "data-platform",
                      "devopsName": "platform-devops"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("data-platform"))
            .andExpect(jsonPath("$.teamName").value("data-platform"))
            .andExpect(jsonPath("$.devopsName").value("platform-devops"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createTeam_shouldRejectDuplicateTeamName() throws Exception {
        mockMvc.perform(withUserId(post("/api/backend/admin/teams"), ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "teamName": "Team A",
                      "devopsName": "duplicate-devops"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Team name already exists"));
    }

    @Test
    void updateTeam_shouldUpdateEntityAndRenameAssignedUsers() throws Exception {
        mockMvc.perform(withUserId(put("/api/backend/admin/teams/team-a"), ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "teamName": "data-platform",
                      "devopsName": "platform-devops-updated"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("data-platform"))
            .andExpect(jsonPath("$.teamName").value("data-platform"))
            .andExpect(jsonPath("$.devopsName").value("platform-devops-updated"));

        mockMvc.perform(withUserId(get("/api/backend/admin/users"), ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.userId=='a')].teamName").value("data-platform"));
    }

    @Test
    void deleteTeam_shouldReturnSuccessWhenNoUsersAssigned() throws Exception {
        mockMvc.perform(withUserId(post("/api/backend/admin/teams"), ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "teamName": "team-to-delete",
                      "devopsName": "delete-devops"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(withUserId(delete("/api/backend/admin/teams/team-to-delete"), ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createUser_shouldPersistAndReturnCreatedEntity() throws Exception {
        mockMvc.perform(withUserId(post("/api/backend/admin/users"), ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "alice",
                      "teamName": "Team A"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("alice"))
            .andExpect(jsonPath("$.userId").value("alice"))
            .andExpect(jsonPath("$.teamName").value("Team A"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createUser_shouldRejectUnknownTeam() throws Exception {
        mockMvc.perform(withUserId(post("/api/backend/admin/users"), ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "alice",
                      "teamName": "missing-team"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Team name must reference an existing team"));
    }

    @Test
    void updateUser_shouldUpdateEntity() throws Exception {
        mockMvc.perform(withUserId(put("/api/backend/admin/users/a"), ADMIN_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "alice",
                      "teamName": "Team B"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("alice"))
            .andExpect(jsonPath("$.userId").value("alice"))
            .andExpect(jsonPath("$.teamName").value("Team B"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void deleteUser_shouldReturnSuccess() throws Exception {
        mockMvc.perform(withUserId(delete("/api/backend/admin/users/a"), ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    private MockHttpServletRequestBuilder withUserId(MockHttpServletRequestBuilder requestBuilder, String userId) {
        return requestBuilder.header(UserIdHeaderFilter.HEADER_NAME, userId);
    }
}

